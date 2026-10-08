package com.riceerp.backend.dto;

import com.riceerp.backend.entity.SalesOrderItem;
import com.riceerp.backend.enums.*;
import java.time.*;
import java.util.List;

/** Explicit public contract. Persistence metadata and back references are not exposed. */
public record SalesOrderItemResponse(
        Long id,
        ProductResponse product,
        Double gstRate,
        int orderedQuantity,
        int packedQuantity,
        int deliveredQuantity,
        int remainingQuantity,
        java.math.BigDecimal unitPrice,
        java.math.BigDecimal totalPrice) {
    public static SalesOrderItemResponse from(SalesOrderItem e) {
        if (e == null) return null;
        return new SalesOrderItemResponse(
                e.getId(),
                ProductResponse.from(e.getProduct()),
                e.getGstRate(),
                e.getOrderedQuantity(),
                e.getPackedQuantity(),
                e.getDeliveredQuantity(),
                e.getRemainingQuantity(),
                e.getUnitPrice(),
                e.getTotalPrice());
    }
}

