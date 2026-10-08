package com.riceerp.backend.dto;

import com.riceerp.backend.entity.SalesOrder;
import com.riceerp.backend.enums.*;
import java.time.*;
import java.util.List;
import java.math.BigDecimal;

/** Explicit public contract. Persistence metadata and back references are not exposed. */
public record SalesOrderResponse(
        Long id,
        String orderNumber,
        CustomerResponse customer,
        UserResponse salesperson,
        LocalDateTime orderDate,
        LocalDate expectedDeliveryDate,
        SalesOrderStatus status,
        TaxType taxType,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal taxAmount,
        BigDecimal grandTotal,
        String notes,
        List<SalesOrderItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long organizationId) {
    public static SalesOrderResponse from(SalesOrder e) {
        if (e == null) return null;
        return new SalesOrderResponse(
                e.getId(),
                e.getOrderNumber(),
                CustomerResponse.from(e.getCustomer()),
                UserResponse.from(e.getSalesperson()),
                e.getOrderDate(),
                e.getExpectedDeliveryDate(),
                e.getStatus(),
                e.getTaxType(),
                e.getSubtotal(),
                e.getDiscount(),
                e.getTaxAmount(),
                e.getGrandTotal(),
                e.getNotes(),
                e.getItems() == null ? List.of() : e.getItems().stream().map(SalesOrderItemResponse::from).toList(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getOrganizationId());
    }
}

