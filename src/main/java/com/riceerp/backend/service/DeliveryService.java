package com.riceerp.backend.service;

import com.riceerp.backend.dto.DeliveryConfirmRequest;
import com.riceerp.backend.dto.DeliveryCreateRequest;
import com.riceerp.backend.dto.DeliveryFailRequest;
import com.riceerp.backend.dto.DeliveryItemConfirmRequest;
import com.riceerp.backend.dto.DeliveryItemCreateRequest;
import com.riceerp.backend.dto.SaleItemRequest;
import com.riceerp.backend.entity.Delivery;
import com.riceerp.backend.entity.DeliveryItem;
import com.riceerp.backend.entity.Product;
import com.riceerp.backend.entity.Sale;
import com.riceerp.backend.entity.SalesOrder;
import com.riceerp.backend.entity.SalesOrderItem;
import com.riceerp.backend.entity.User;
import com.riceerp.backend.enums.DeliveryStatus;
import com.riceerp.backend.enums.PaymentMode;
import com.riceerp.backend.enums.SalesOrderStatus;
import com.riceerp.backend.exception.BusinessRuleException;
import com.riceerp.backend.exception.NotFoundException;
import com.riceerp.backend.repository.DeliveryItemRepository;
import com.riceerp.backend.repository.DeliveryRepository;
import com.riceerp.backend.repository.ProductRepository;
import com.riceerp.backend.repository.SalesOrderItemRepository;
import com.riceerp.backend.repository.SalesOrderRepository;
import com.riceerp.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryItemRepository deliveryItemRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final SaleService saleService;

    public DeliveryService(DeliveryRepository deliveryRepository,
                           DeliveryItemRepository deliveryItemRepository,
                           SalesOrderRepository salesOrderRepository,
                           SalesOrderItemRepository salesOrderItemRepository,
                           ProductRepository productRepository,
                           UserRepository userRepository,
                           SaleService saleService) {
        this.deliveryRepository = deliveryRepository;
        this.deliveryItemRepository = deliveryItemRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.salesOrderItemRepository = salesOrderItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.saleService = saleService;
    }

    @Transactional
    public Delivery createDeliveryNote(DeliveryCreateRequest request) {
        SalesOrder order = salesOrderRepository.findById(request.getSalesOrderId())
                .orElseThrow(() -> new NotFoundException("Sales order not found with id: " + request.getSalesOrderId()));

        if (order.getStatus() == SalesOrderStatus.DELIVERED || order.getStatus() == SalesOrderStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot create delivery note for an order that is " + order.getStatus());
        }

        User deliveryPerson = null;
        if (request.getDeliveryPersonId() != null) {
            deliveryPerson = userRepository.findById(request.getDeliveryPersonId()).orElse(null);
        }

        Delivery delivery = new Delivery();
        delivery.setDeliveryNumber("DN-" + System.currentTimeMillis());
        delivery.setSalesOrder(order);
        delivery.setDeliveryPerson(deliveryPerson);
        delivery.setVehicleNumber(request.getVehicleNumber());
        delivery.setStatus(DeliveryStatus.ASSIGNED);
        delivery.setAssignedAt(LocalDateTime.now());
        delivery.setDeliveryNotes(request.getDeliveryNotes());

        List<DeliveryItem> deliveryItems = new ArrayList<>();

        for (DeliveryItemCreateRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new NotFoundException("Product not found with id: " + itemReq.getProductId()));

            // Find matching SalesOrderItem
            SalesOrderItem orderItem = order.getItems().stream()
                    .filter(oi -> oi.getProduct().getId().equals(product.getId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessRuleException("Product " + product.getProductName() + " is not in the sales order"));

            if (itemReq.getDeliveringQuantity() > orderItem.getRemainingQuantity()) {
                throw new BusinessRuleException("Delivering quantity (" + itemReq.getDeliveringQuantity()
                        + ") exceeds remaining ordered quantity (" + orderItem.getRemainingQuantity() + ") for " + product.getProductName());
            }

            DeliveryItem dItem = new DeliveryItem();
            dItem.setDelivery(delivery);
            dItem.setProduct(product);
            dItem.setOrderedQuantity(orderItem.getOrderedQuantity());
            dItem.setDeliveringQuantity(itemReq.getDeliveringQuantity());
            dItem.setDeliveredQuantity(0);
            dItem.setUnitPrice(orderItem.getUnitPrice());

            deliveryItems.add(dItem);

            // Update packed quantity on order item
            orderItem.setPackedQuantity(orderItem.getPackedQuantity() + itemReq.getDeliveringQuantity());
            salesOrderItemRepository.save(orderItem);
        }

        delivery.setItems(deliveryItems);
        order.setStatus(SalesOrderStatus.READY_FOR_DELIVERY);
        order.setUpdatedAt(LocalDateTime.now());
        salesOrderRepository.save(order);

        return deliveryRepository.save(delivery);
    }

    public Delivery getDeliveryById(Long id) {
        return deliveryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Delivery not found with id: " + id));
    }

    public List<Delivery> listDeliveries(DeliveryStatus status) {
        if (status != null) {
            return deliveryRepository.findByStatusOrderByAssignedAtDesc(status);
        }
        return deliveryRepository.findAllByOrderByAssignedAtDesc();
    }

    public List<Delivery> getMyDeliveries(Long deliveryPersonId, DeliveryStatus status) {
        if (status != null) {
            return deliveryRepository.findByDeliveryPersonIdAndStatusOrderByAssignedAtDesc(deliveryPersonId, status);
        }
        return deliveryRepository.findByDeliveryPersonIdOrderByAssignedAtDesc(deliveryPersonId);
    }

    @Transactional
    public Delivery startDelivery(Long deliveryId) {
        Delivery delivery = getDeliveryById(deliveryId);
        if (delivery.getStatus() != DeliveryStatus.ASSIGNED) {
            throw new BusinessRuleException("Delivery has already been started or closed.");
        }

        delivery.setStatus(DeliveryStatus.OUT_FOR_DELIVERY);
        delivery.setStartedAt(LocalDateTime.now());

        SalesOrder order = delivery.getSalesOrder();
        order.setStatus(SalesOrderStatus.OUT_FOR_DELIVERY);
        order.setUpdatedAt(LocalDateTime.now());
        salesOrderRepository.save(order);

        return deliveryRepository.save(delivery);
    }

    @Transactional
    public Delivery confirmDelivery(Long deliveryId, DeliveryConfirmRequest request) {
        Delivery delivery = getDeliveryById(deliveryId);
        if (delivery.getStatus() == DeliveryStatus.DELIVERED) {
            throw new BusinessRuleException("Delivery has already been finalized and invoiced.");
        }

        delivery.setDeliveredAt(LocalDateTime.now());
        delivery.setReceiverName(request.getReceiverName());
        delivery.setReceiverPhone(request.getReceiverPhone());
        if (request.getDeliveryNotes() != null) {
            delivery.setDeliveryNotes(request.getDeliveryNotes());
        }

        SalesOrder order = delivery.getSalesOrder();
        List<SaleItemRequest> invoiceItems = new ArrayList<>();
        boolean isPartialDelivery = false;

        for (DeliveryItemConfirmRequest confItem : request.getItems()) {
            DeliveryItem dItem = delivery.getItems().stream()
                    .filter(di -> di.getProduct().getId().equals(confItem.getProductId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessRuleException("Item not found in delivery note: " + confItem.getProductId()));

            int actualDelivered = confItem.getDeliveredQuantity();
            if (actualDelivered < dItem.getDeliveringQuantity()) {
                isPartialDelivery = true;
            }

            dItem.setDeliveredQuantity(actualDelivered);
            deliveryItemRepository.save(dItem);

            // Update corresponding SalesOrderItem
            SalesOrderItem orderItem = order.getItems().stream()
                    .filter(oi -> oi.getProduct().getId().equals(confItem.getProductId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessRuleException("Product not found in sales order"));

            orderItem.setDeliveredQuantity(orderItem.getDeliveredQuantity() + actualDelivered);
            orderItem.setRemainingQuantity(Math.max(0, orderItem.getOrderedQuantity() - orderItem.getDeliveredQuantity()));
            salesOrderItemRepository.save(orderItem);

            // Add to invoice if delivered > 0
            if (actualDelivered > 0) {
                SaleItemRequest sItem = new SaleItemRequest();
                sItem.setProductId(dItem.getProduct().getId());
                sItem.setQuantity(actualDelivered);
                sItem.setPrice(dItem.getUnitPrice());
                invoiceItems.add(sItem);
            }
        }

        delivery.setStatus(isPartialDelivery ? DeliveryStatus.PARTIALLY_DELIVERED : DeliveryStatus.DELIVERED);

        // Check if overall sales order is complete
        boolean allItemsFulfilled = order.getItems().stream().allMatch(oi -> oi.getRemainingQuantity() == 0);
        order.setStatus(allItemsFulfilled ? SalesOrderStatus.DELIVERED : SalesOrderStatus.PARTIALLY_DELIVERED);
        order.setUpdatedAt(LocalDateTime.now());
        salesOrderRepository.save(order);

        // 🧾 Automatically Generate Invoice for Actual Delivered Goods
        if (!invoiceItems.isEmpty()) {
            PaymentMode pMode = request.getPaymentMode() != null ? request.getPaymentMode() : PaymentMode.CREDIT;
            Sale generatedSale = saleService.createSaleFromDelivery(
                    delivery.getId(),
                    order.getId(),
                    order.getCustomer(),
                    invoiceItems,
                    pMode,
                    0.0
            );
            delivery.setGeneratedInvoiceId(generatedSale.getId());
        }

        return deliveryRepository.save(delivery);
    }

    @Transactional
    public Delivery markDeliveryFailed(Long deliveryId, DeliveryFailRequest request) {
        Delivery delivery = getDeliveryById(deliveryId);
        delivery.setStatus(DeliveryStatus.FAILED);
        delivery.setFailureReason(request.getFailureReason());
        if (request.getDeliveryNotes() != null) {
            delivery.setDeliveryNotes(request.getDeliveryNotes());
        }

        SalesOrder order = delivery.getSalesOrder();
        // Revert sales order status back to READY_FOR_DELIVERY so it can be rescheduled or reassigned
        order.setStatus(SalesOrderStatus.READY_FOR_DELIVERY);
        order.setUpdatedAt(LocalDateTime.now());
        salesOrderRepository.save(order);

        return deliveryRepository.save(delivery);
    }
}
