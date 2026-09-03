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

    public SalesOrderService(SalesOrderRepository salesOrderRepository,
                             SalesOrderItemRepository salesOrderItemRepository,
                             CustomerRepository customerRepository,
                             ProductRepository productRepository,
                             UserRepository userRepository) {
        this.salesOrderRepository = salesOrderRepository;
        this.salesOrderItemRepository = salesOrderItemRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public SalesOrder createSalesOrder(SalesOrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessRuleException("Sales order must contain at least one item.");
        }

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new NotFoundException("Customer not found with id: " + request.getCustomerId()));

        User salesperson = null;
        if (request.getSalespersonId() != null) {
            salesperson = userRepository.findById(request.getSalespersonId()).orElse(null);
        }

        double subtotal = 0.0;
        List<SalesOrderItem> orderItems = new ArrayList<>();

        SalesOrder order = new SalesOrder();
        order.setOrderNumber("SO-" + System.currentTimeMillis());
        order.setCustomer(customer);
        order.setSalesperson(salesperson);
        order.setOrderDate(LocalDateTime.now());
        order.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
        order.setStatus(SalesOrderStatus.CONFIRMED);
        order.setDiscount(request.getDiscount());
        order.setNotes(request.getNotes());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        for (SalesOrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new NotFoundException("Product not found with id: " + itemReq.getProductId()));

            double price = itemReq.getUnitPrice() > 0 ? itemReq.getUnitPrice() : product.getSellingPrice();
            double itemTotal = itemReq.getQuantity() * price;
            subtotal += itemTotal;

            SalesOrderItem orderItem = new SalesOrderItem();
            orderItem.setSalesOrder(order);
            orderItem.setProduct(product);
            orderItem.setOrderedQuantity(itemReq.getQuantity());
            orderItem.setPackedQuantity(0);
            orderItem.setDeliveredQuantity(0);
            orderItem.setRemainingQuantity(itemReq.getQuantity());
            orderItem.setUnitPrice(price);
            orderItem.setTotalPrice(itemTotal);

            orderItems.add(orderItem);
        }

        double netTotal = Math.max(0.0, subtotal - request.getDiscount());
        double taxAmount = netTotal * 0.05; // 5% GST (2.5% CGST + 2.5% SGST)
        double grandTotal = netTotal + taxAmount;

        order.setSubtotal(subtotal);
        order.setTaxAmount(taxAmount);
        order.setGrandTotal(grandTotal);
        order.setItems(orderItems);

        return salesOrderRepository.save(order);
    }

    public SalesOrder getSalesOrderById(Long id) {
        return salesOrderRepository.findById(id)
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

    @Transactional
    public SalesOrder updateStatus(Long id, SalesOrderStatus newStatus) {
        SalesOrder order = getSalesOrderById(id);
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        return salesOrderRepository.save(order);
    }

    @Transactional
    public SalesOrder cancelSalesOrder(Long id, String reason) {
        SalesOrder order = getSalesOrderById(id);
        if (order.getStatus() == SalesOrderStatus.DELIVERED) {
            throw new BusinessRuleException("Cannot cancel a delivered sales order.");
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
            int currentStock = (int) product.getStock();
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
