package com.riceerp.backend.service;

import com.riceerp.backend.dto.SalesOrderItemRequest;
import com.riceerp.backend.dto.SalesOrderRequest;
import com.riceerp.backend.entity.Customer;
import com.riceerp.backend.entity.Product;
import com.riceerp.backend.entity.SalesOrder;
import com.riceerp.backend.entity.SalesOrderItem;
import com.riceerp.backend.entity.User;
import com.riceerp.backend.enums.SalesOrderStatus;
import com.riceerp.backend.exception.BusinessRuleException;
import com.riceerp.backend.exception.NotFoundException;
import com.riceerp.backend.repository.CustomerRepository;
import com.riceerp.backend.repository.ProductRepository;
import com.riceerp.backend.repository.SalesOrderItemRepository;
import com.riceerp.backend.repository.SalesOrderRepository;
import com.riceerp.backend.repository.UserRepository;
import com.riceerp.backend.repository.OrganizationRepository;
import com.riceerp.backend.repository.DeliveryRepository;
import com.riceerp.backend.security.TenantContext;
import com.riceerp.backend.enums.DeliveryStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Isolation;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SalesOrderService {

    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizations;
    private final DeliveryRepository deliveries;

    public SalesOrderService(SalesOrderRepository salesOrderRepository,
                             SalesOrderItemRepository salesOrderItemRepository,
                             CustomerRepository customerRepository,
                             ProductRepository productRepository,
                             UserRepository userRepository, OrganizationRepository organizations, DeliveryRepository deliveries) {
        this.salesOrderRepository = salesOrderRepository;
        this.salesOrderItemRepository = salesOrderItemRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.organizations = organizations;
        this.deliveries = deliveries;
    }

    private Long tenant() {
        Long orgId = TenantContext.getCurrentTenant();
        if (orgId == null || orgId <= 0) throw new AccessDeniedException("Select an organization first");
        return orgId;
    }

    private void lockMutations() {
        // Same lock as dispatch/confirmation: cancellation cannot pass a stale check.
        organizations.lockForDelivery(tenant()).orElseThrow(() -> new NotFoundException("Organization not found"));
    }

    private boolean hasActiveDelivery(Long id) {
        return deliveries.findBySalesOrderIdAndOrganizationId(id, tenant()).stream().anyMatch(note ->
                note.getStatus() == DeliveryStatus.ASSIGNED || note.getStatus() == DeliveryStatus.OUT_FOR_DELIVERY);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SalesOrder createSalesOrder(SalesOrderRequest request) {
        lockMutations();
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessRuleException("Sales order must contain at least one item.");
        }

        Customer customer = customerRepository.findByIdAndOrganizationId(request.getCustomerId(), tenant())
                .orElseThrow(() -> new NotFoundException("Customer not found with id: " + request.getCustomerId()));

        User salesperson = null;
        if (request.getSalespersonId() != null) {
            salesperson = userRepository.findActiveOrganizationUser(request.getSalespersonId(), tenant())
                    .orElseThrow(() -> new BusinessRuleException("Salesperson must be an active member of this shop"));
        }

        BigDecimal subtotalValue = BigDecimal.ZERO;
        Set<Long> productIds = new HashSet<>();
        List<SalesOrderItem> orderItems = new ArrayList<>();

        SalesOrder order = new SalesOrder();
        order.setOrderNumber("SO-" + UUID.randomUUID());
        order.setCustomer(customer);
        order.setSalesperson(salesperson);
        order.setOrderDate(LocalDateTime.now());
        order.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
        order.setStatus(SalesOrderStatus.CONFIRMED);
        order.setNotes(request.getNotes());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        for (SalesOrderItemRequest itemReq : request.getItems()) {
            if (itemReq == null || itemReq.getProductId() == null || !productIds.add(itemReq.getProductId()))
                throw new BusinessRuleException("Each order product must appear exactly once.");
            if (itemReq.getQuantity() <= 0 || itemReq.getUnitPrice() == null || itemReq.getUnitPrice().signum() < 0)
                throw new BusinessRuleException("Order quantities must be positive and prices finite and non-negative.");
            Product product = productRepository.findForStockUpdate(itemReq.getProductId())
                    .orElseThrow(() -> new NotFoundException("Product not found with id: " + itemReq.getProductId()));

            double available = product.getStock() - productRepository.reservedQuantity(product.getId());
            if (available < itemReq.getQuantity())
                throw new BusinessRuleException("Insufficient unreserved stock for " + product.getProductName() + ". Available: " + Math.max(0, available));

            BigDecimal unitPrice = itemReq.getUnitPrice().signum() > 0 ? OrderBilling.money(itemReq.getUnitPrice()) : OrderBilling.money(product.getSellingPrice());
            if (unitPrice.signum() <= 0) throw new BusinessRuleException("Order price must be greater than zero.");
            BigDecimal itemAmount = unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            subtotalValue = subtotalValue.add(itemAmount);

            SalesOrderItem orderItem = new SalesOrderItem();
            orderItem.setSalesOrder(order);
            orderItem.setProduct(product);
            Double gstRate = GstCalculator.rate(product.getGstRate());
            if (itemReq.isGstRateProvided() && !java.util.Objects.equals(GstCalculator.rate(itemReq.getGstRate()), gstRate))
                throw new BusinessRuleException("Product GST changed. Refresh this product and review the order before submitting.");
            orderItem.setGstRate(gstRate);
            orderItem.setOrderedQuantity(itemReq.getQuantity());
            orderItem.setPackedQuantity(0);
            orderItem.setDeliveredQuantity(0);
            orderItem.setRemainingQuantity(itemReq.getQuantity());
            orderItem.setUnitPrice(unitPrice);
            orderItem.setTotalPrice(itemAmount);

            orderItems.add(orderItem);
        }

        BigDecimal discount = OrderBilling.money(request.getDiscount());
        if (discount.signum() < 0 || discount.compareTo(subtotalValue) > 0)
            throw new BusinessRuleException("Order discount must be between zero and the subtotal.");
        BigDecimal netTotal = subtotalValue.subtract(discount);
        BigDecimal taxValue = GstCalculator.calculate(orderItems.stream().map(item -> new GstCalculator.Line(
                item.getUnitPrice().multiply(BigDecimal.valueOf(item.getOrderedQuantity())),
                item.getGstRate())).toList(), discount, request.getTaxType()).total();
        BigDecimal grandTotal = netTotal.add(taxValue);
        BigDecimal maximum = new BigDecimal("999999999999999.9999");
        if (subtotalValue.compareTo(maximum) > 0 || grandTotal.compareTo(maximum) > 0)
            throw new BusinessRuleException("Order total is too large.");

        order.setSubtotal(subtotalValue);
        order.setDiscount(discount);
        order.setTaxType(request.getTaxType());
        order.setTaxAmount(taxValue);
        order.setGrandTotal(grandTotal);
        order.setItems(orderItems);

        return salesOrderRepository.save(order);
    }

    public SalesOrder getSalesOrderById(Long id) {
        return salesOrderRepository.findByIdAndOrganizationId(id, tenant())
                .orElseThrow(() -> new NotFoundException("Sales order not found with id: " + id));
    }

    public List<SalesOrder> listSalesOrders(SalesOrderStatus status, Long customerId, Long salespersonId) {
        if (status != null) {
            return salesOrderRepository.findByStatusOrderByOrderDateDesc(status);
        }
        if (customerId != null) {
            return salesOrderRepository.findByCustomerIdOrderByOrderDateDesc(customerId);
        }
        if (salespersonId != null) {
            return salesOrderRepository.findBySalespersonIdOrderByOrderDateDesc(salespersonId);
        }
        return salesOrderRepository.findAllByOrderByOrderDateDesc();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SalesOrder updateStatus(Long id, SalesOrderStatus newStatus) {
        lockMutations();
        SalesOrder order = getSalesOrderById(id);
        boolean valid = (order.getStatus() == SalesOrderStatus.CONFIRMED &&
                (newStatus == SalesOrderStatus.PROCESSING || newStatus == SalesOrderStatus.READY_FOR_DELIVERY)) ||
                (order.getStatus() == SalesOrderStatus.PROCESSING && newStatus == SalesOrderStatus.READY_FOR_DELIVERY) ||
                (order.getStatus() == SalesOrderStatus.PARTIALLY_DELIVERED &&
                (newStatus == SalesOrderStatus.PROCESSING || newStatus == SalesOrderStatus.READY_FOR_DELIVERY));
        if (!valid || hasActiveDelivery(id))
            throw new BusinessRuleException("Invalid order transition. Delivery states come from delivery notes; cancellation uses the cancel action.");
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        return salesOrderRepository.save(order);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SalesOrder cancelSalesOrder(Long id, String reason) {
        lockMutations();
        SalesOrder order = getSalesOrderById(id);
        if (order.getStatus() == SalesOrderStatus.DELIVERED || order.getStatus() == SalesOrderStatus.CANCELLED ||
                order.getItems().stream().anyMatch(item -> item.getDeliveredQuantity() > 0) || hasActiveDelivery(id) ||
                deliveries.findBySalesOrderIdAndOrganizationId(id, tenant()).stream().anyMatch(note -> note.getGeneratedInvoiceId() != null)) {
            throw new BusinessRuleException("Cannot cancel a closed, dispatched or partially fulfilled order. Resolve active deliveries first.");
        }
        order.setStatus(SalesOrderStatus.CANCELLED);
        if (reason != null && !reason.trim().isEmpty()) {
            order.setNotes((order.getNotes() != null ? order.getNotes() + " | Cancellation Reason: " : "Cancelled: ") + reason);
        }
        order.setUpdatedAt(LocalDateTime.now());
        return salesOrderRepository.save(order);
    }

    public Map<String, Object> checkStockAvailability(Long salesOrderId) {
        SalesOrder order = getSalesOrderById(salesOrderId);
        List<Map<String, Object>> itemStockStatus = new ArrayList<>();
        boolean allAvailable = true;

        for (SalesOrderItem item : order.getItems()) {
            Product product = item.getProduct();
            int needed = item.getRemainingQuantity();
            double ownReservation = order.getStatus() == SalesOrderStatus.CANCELLED || order.getStatus() == SalesOrderStatus.DRAFT || order.getStatus() == SalesOrderStatus.DELIVERED ? 0 : needed;
            double currentStock = Math.max(0, product.getStock() - productRepository.reservedQuantity(product.getId()) + ownReservation);
            boolean isAvailable = currentStock >= needed;

            if (!isAvailable) {
                allAvailable = false;
            }

            Map<String, Object> statusMap = new HashMap<>();
            statusMap.put("productId", product.getId());
            statusMap.put("productName", product.getProductName());
            statusMap.put("orderedQuantity", item.getOrderedQuantity());
            statusMap.put("remainingQuantity", needed);
            statusMap.put("availableStock", currentStock);
            statusMap.put("isAvailable", isAvailable);
            statusMap.put("shortage", isAvailable ? 0 : (needed - currentStock));

            itemStockStatus.add(statusMap);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("salesOrderId", order.getId());
        result.put("orderNumber", order.getOrderNumber());
        result.put("allAvailable", allAvailable);
        result.put("items", itemStockStatus);
        return result;
    }

    public Map<String, Long> getFulfillmentCounts() {
        Map<String, Long> counts = new HashMap<>();
        counts.put("NEW_ORDERS", salesOrderRepository.countByStatus(SalesOrderStatus.CONFIRMED));
        counts.put("PROCESSING", salesOrderRepository.countByStatus(SalesOrderStatus.PROCESSING));
        counts.put("READY", salesOrderRepository.countByStatus(SalesOrderStatus.READY_FOR_DELIVERY));
        counts.put("OUT_FOR_DELIVERY", salesOrderRepository.countByStatus(SalesOrderStatus.OUT_FOR_DELIVERY));
        counts.put("PARTIALLY_DELIVERED", salesOrderRepository.countByStatus(SalesOrderStatus.PARTIALLY_DELIVERED));
        counts.put("DELIVERED", salesOrderRepository.countByStatus(SalesOrderStatus.DELIVERED));
        return counts;
    }
}
