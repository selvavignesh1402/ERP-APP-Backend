package com.riceerp.backend.service;

import com.riceerp.backend.dto.*;
import com.riceerp.backend.entity.*;
import com.riceerp.backend.enums.*;
import com.riceerp.backend.exception.BusinessRuleException;
import com.riceerp.backend.exception.NotFoundException;
import com.riceerp.backend.repository.*;
import com.riceerp.backend.security.TenantContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class DeliveryService {
    private final DeliveryRepository deliveryRepository;
    private final DeliveryItemRepository deliveryItemRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final UserRepository userRepository;
    private final SaleService saleService;
    private final OrganizationRepository organizations;
    private final SaleRepository invoices;

    public DeliveryService(DeliveryRepository deliveryRepository, DeliveryItemRepository deliveryItemRepository,
            SalesOrderRepository salesOrderRepository, SalesOrderItemRepository salesOrderItemRepository,
            ProductRepository productRepository, UserRepository userRepository, SaleService saleService,
            OrganizationRepository organizations, SaleRepository invoices) {
        this.deliveryRepository = deliveryRepository;
        this.deliveryItemRepository = deliveryItemRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.salesOrderItemRepository = salesOrderItemRepository;
        this.userRepository = userRepository;
        this.saleService = saleService;
        this.organizations = organizations;
        this.invoices = invoices;
    }

    private Long tenant() {
        Long id = TenantContext.getCurrentTenant();
        if (id == null || id <= 0) throw new AccessDeniedException("Select an organization first");
        return id;
    }

    // Acquire before loading delivery/order entities. READ_COMMITTED ensures that a
    // waiting confirmation sees the first transaction's committed terminal status.
    private void lockMutations() {
        organizations.lockForDelivery(tenant()).orElseThrow(() -> new NotFoundException("Organization not found"));
    }

    private boolean active(Delivery delivery) {
        return delivery.getStatus() == DeliveryStatus.ASSIGNED || delivery.getStatus() == DeliveryStatus.OUT_FOR_DELIVERY;
    }

    private void assertOpenOrder(SalesOrder order) {
        if (order.getStatus() == SalesOrderStatus.CANCELLED || order.getStatus() == SalesOrderStatus.DELIVERED)
            throw new BusinessRuleException("The sales order is already closed.");
    }

    private Map<Long, SalesOrderItem> orderItems(SalesOrder order) {
        Map<Long, SalesOrderItem> result = new LinkedHashMap<>();
        for (SalesOrderItem item : order.getItems()) {
            if (result.putIfAbsent(item.getProduct().getId(), item) != null)
                throw new BusinessRuleException("This order has duplicate product lines. Resolve them before dispatching.");
        }
        return result;
    }

    private List<Delivery> siblings(SalesOrder order) {
        return deliveryRepository.findBySalesOrderIdAndOrganizationId(order.getId(), tenant());
    }

    private long reserved(List<Delivery> notes, Long productId) {
        return notes.stream().filter(this::active).flatMap(note -> note.getItems().stream())
                .filter(item -> item.getProduct().getId().equals(productId))
                .mapToLong(DeliveryItem::getDeliveringQuantity).sum();
    }

    // Packed quantity represents fulfilled quantities plus outstanding dispatches.
    // Closing a partial/failed note releases its undelivered allocation.
    private void refreshOrder(SalesOrder order, List<Delivery> notes) {
        for (SalesOrderItem item : order.getItems()) {
            long packed = item.getDeliveredQuantity() + reserved(notes, item.getProduct().getId());
            if (packed > item.getOrderedQuantity() || packed < 0)
                throw new BusinessRuleException("Delivery allocations exceed this order. Reconcile existing delivery notes.");
            item.setPackedQuantity((int) packed);
            item.setRemainingQuantity(item.getOrderedQuantity() - item.getDeliveredQuantity());
            salesOrderItemRepository.save(item);
        }
        if (order.getItems().stream().allMatch(item -> item.getRemainingQuantity() == 0)) {
            order.setStatus(SalesOrderStatus.DELIVERED);
        } else if (notes.stream().anyMatch(note -> note.getStatus() == DeliveryStatus.OUT_FOR_DELIVERY)) {
            order.setStatus(SalesOrderStatus.OUT_FOR_DELIVERY);
        } else if (notes.stream().anyMatch(note -> note.getStatus() == DeliveryStatus.ASSIGNED)) {
            order.setStatus(SalesOrderStatus.READY_FOR_DELIVERY);
        } else if (order.getItems().stream().anyMatch(item -> item.getDeliveredQuantity() > 0)) {
            order.setStatus(SalesOrderStatus.PARTIALLY_DELIVERED);
        } else {
            order.setStatus(SalesOrderStatus.CONFIRMED);
        }
        order.setUpdatedAt(LocalDateTime.now());
        salesOrderRepository.save(order);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Delivery createDeliveryNote(DeliveryCreateRequest request) {
        lockMutations();
        SalesOrder order = salesOrderRepository.findByIdAndOrganizationId(request.getSalesOrderId(), tenant())
                .orElseThrow(() -> new NotFoundException("Sales order not found"));
        assertOpenOrder(order);
        if (request.getItems() == null || request.getItems().isEmpty())
            throw new BusinessRuleException("Specify at least one dispatch item.");
        Map<Long, SalesOrderItem> ordered = orderItems(order);
        List<Delivery> notes = new ArrayList<>(siblings(order));
        Set<Long> seen = new HashSet<>();
        for (DeliveryItemCreateRequest item : request.getItems()) {
            if (item == null || item.getProductId() == null || !seen.add(item.getProductId()))
                throw new BusinessRuleException("Each dispatch product must appear exactly once.");
            SalesOrderItem line = ordered.get(item.getProductId());
            if (line == null) throw new BusinessRuleException("Product is not part of the sales order.");
            long available = (long) line.getOrderedQuantity() - line.getDeliveredQuantity() - reserved(notes, item.getProductId());
            if (item.getDeliveringQuantity() <= 0 || item.getDeliveringQuantity() > available)
                throw new BusinessRuleException("Dispatch quantity exceeds unallocated order quantity for " +
                        line.getProduct().getProductName() + ". Available: " + available);
        }
        User person = request.getDeliveryPersonId() == null ? null : userRepository.findActiveOrganizationUser(request.getDeliveryPersonId(), tenant())
                .orElseThrow(() -> new NotFoundException("Delivery person not found"));
        Delivery delivery = new Delivery();
        delivery.setDeliveryNumber("DN-" + UUID.randomUUID());
        delivery.setSalesOrder(order);
        delivery.setDeliveryPerson(person);
        delivery.setVehicleNumber(request.getVehicleNumber());
        delivery.setStatus(DeliveryStatus.ASSIGNED);
        delivery.setAssignedAt(LocalDateTime.now());
        delivery.setDeliveryNotes(request.getDeliveryNotes());
        List<DeliveryItem> items = new ArrayList<>();
        for (DeliveryItemCreateRequest requested : request.getItems()) {
            SalesOrderItem line = ordered.get(requested.getProductId());
            DeliveryItem item = new DeliveryItem();
            item.setDelivery(delivery); item.setProduct(line.getProduct());
            item.setOrderedQuantity(line.getOrderedQuantity());
            item.setDeliveringQuantity(requested.getDeliveringQuantity());
            item.setDeliveredQuantity(0); item.setUnitPrice(line.getUnitPrice());
            items.add(item);
        }
        delivery.setItems(items);
        Delivery saved = deliveryRepository.save(delivery);
        notes.add(saved);
        refreshOrder(order, notes);
        return saved;
    }

    public Delivery getDeliveryById(Long id) {
        return deliveryRepository.findByIdAndOrganizationId(id, tenant())
                .orElseThrow(() -> new NotFoundException("Delivery not found"));
    }

    public List<Delivery> listDeliveries(DeliveryStatus status) {
        return status == null ? deliveryRepository.findByOrganizationIdOrderByAssignedAtDesc(tenant())
                : deliveryRepository.findByStatusAndOrganizationIdOrderByAssignedAtDesc(status, tenant());
    }

    public List<Delivery> getMyDeliveries(Long userId, DeliveryStatus status) {
        return status == null ? deliveryRepository.findByDeliveryPersonIdAndOrganizationIdOrderByAssignedAtDesc(userId, tenant())
                : deliveryRepository.findByDeliveryPersonIdAndStatusAndOrganizationIdOrderByAssignedAtDesc(userId, status, tenant());
    }

    private void assertDeliveryAccess(Delivery delivery, Long userId, boolean privileged) {
        if (!privileged && (delivery.getDeliveryPerson() == null || !Objects.equals(delivery.getDeliveryPerson().getId(), userId)))
            throw new AccessDeniedException("This delivery is not assigned to you.");
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Delivery startDelivery(Long id, Long userId, boolean privileged) {
        lockMutations();
        Delivery delivery = getDeliveryById(id);
        assertDeliveryAccess(delivery, userId, privileged);
        assertOpenOrder(delivery.getSalesOrder());
        if (delivery.getStatus() != DeliveryStatus.ASSIGNED)
            throw new BusinessRuleException("Only an assigned delivery can be started.");
        delivery.setStatus(DeliveryStatus.OUT_FOR_DELIVERY);
        delivery.setStartedAt(LocalDateTime.now());
        refreshOrder(delivery.getSalesOrder(), siblings(delivery.getSalesOrder()));
        return deliveryRepository.save(delivery);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Delivery confirmDelivery(Long id, DeliveryConfirmRequest request, Long userId, boolean privileged) {
        lockMutations();
        Delivery delivery = getDeliveryById(id);
        assertDeliveryAccess(delivery, userId, privileged);
        if (delivery.getStatus() != DeliveryStatus.OUT_FOR_DELIVERY || delivery.getGeneratedInvoiceId() != null)
            throw new BusinessRuleException("Only an in-transit delivery can be confirmed once. Dispatch remaining goods on a new delivery note.");
        SalesOrder order = delivery.getSalesOrder();
        assertOpenOrder(order);
        Map<Long, SalesOrderItem> ordered = orderItems(order);
        if (request.getItems() == null || request.getItems().isEmpty() ||
                request.getReceiverName() == null || request.getReceiverName().isBlank())
            throw new BusinessRuleException("Receiver name and all delivered quantities are required.");
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (DeliveryItemConfirmRequest item : request.getItems()) {
            if (item == null || item.getProductId() == null || item.getDeliveredQuantity() < 0 ||
                    quantities.putIfAbsent(item.getProductId(), item.getDeliveredQuantity()) != null)
                throw new BusinessRuleException("Provide each delivery product once with a non-negative quantity.");
        }
        Set<Long> noteProducts = new HashSet<>();
        for (DeliveryItem item : delivery.getItems()) {
            Long productId = item.getProduct().getId();
            Integer actual = quantities.get(productId);
            SalesOrderItem line = ordered.get(productId);
            if (!noteProducts.add(productId) || actual == null || line == null)
                throw new BusinessRuleException("Confirm every dispatched product explicitly, including zero for undelivered goods.");
            if (actual > item.getDeliveringQuantity() || actual > (long) line.getOrderedQuantity() - line.getDeliveredQuantity())
                throw new BusinessRuleException("Delivered quantity exceeds the dispatch or remaining order quantity.");
        }
        if (!noteProducts.equals(quantities.keySet()) || noteProducts.isEmpty())
            throw new BusinessRuleException("Confirmation contains products outside this delivery.");

        // Validation is complete before any quantity, proof or invoice is changed.
        List<SaleItemRequest> invoiceItems = new ArrayList<>();
        boolean partial = false;
        for (DeliveryItem item : delivery.getItems()) {
            int actual = quantities.get(item.getProduct().getId());
            partial |= actual < item.getDeliveringQuantity();
            item.setDeliveredQuantity(actual);
            deliveryItemRepository.save(item);
            SalesOrderItem line = ordered.get(item.getProduct().getId());
            line.setDeliveredQuantity(line.getDeliveredQuantity() + actual);
            if (actual > 0) {
                SaleItemRequest invoice = new SaleItemRequest();
                invoice.setProductId(item.getProduct().getId()); invoice.setQuantity(actual); invoice.setPrice(item.getUnitPrice());
                invoiceItems.add(invoice);
            }
        }
        delivery.setStatus(partial ? DeliveryStatus.PARTIALLY_DELIVERED : DeliveryStatus.DELIVERED);
        delivery.setDeliveredAt(LocalDateTime.now());
        delivery.setReceiverName(request.getReceiverName()); delivery.setReceiverPhone(request.getReceiverPhone());
        if (request.getDeliveryNotes() != null) delivery.setDeliveryNotes(request.getDeliveryNotes());
        refreshOrder(order, siblings(order));
        // Flush delivered quantities before the native reservation query in invoice creation.
        salesOrderRepository.flush();
        if (!invoiceItems.isEmpty()) {
            java.math.BigDecimal discount = OrderBilling.discountFor(order, invoiceItems,
                    invoices.findBySalesOrderIdAndOrganizationId(order.getId(), tenant()));
            var gstSnapshots = new java.util.HashMap<Long, Double>();
            for (var item : order.getItems()) gstSnapshots.put(item.getProduct().getId(), item.getGstRate());
            Sale sale = saleService.createSaleFromDelivery(id, order.getId(), order.getCustomer(), invoiceItems,
                    request.getPaymentMode() == null ? PaymentMode.CREDIT : request.getPaymentMode(), discount,
                    order.getTaxType(), gstSnapshots);
            delivery.setGeneratedInvoiceId(sale.getId());
        }
        return deliveryRepository.save(delivery);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Delivery markDeliveryFailed(Long id, DeliveryFailRequest request, Long userId, boolean privileged) {
        lockMutations();
        Delivery delivery = getDeliveryById(id);
        assertDeliveryAccess(delivery, userId, privileged);
        assertOpenOrder(delivery.getSalesOrder());
        if (!active(delivery) || delivery.getGeneratedInvoiceId() != null)
            throw new BusinessRuleException("A closed delivery cannot be marked as failed.");
        if (request.getFailureReason() == null) throw new BusinessRuleException("Failure reason is required.");
        delivery.setStatus(DeliveryStatus.FAILED);
        delivery.setFailureReason(request.getFailureReason());
        if (request.getDeliveryNotes() != null) delivery.setDeliveryNotes(request.getDeliveryNotes());
        refreshOrder(delivery.getSalesOrder(), siblings(delivery.getSalesOrder()));
        return deliveryRepository.save(delivery);
    }
}
