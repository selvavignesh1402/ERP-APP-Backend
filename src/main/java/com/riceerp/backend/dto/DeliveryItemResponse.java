package com.riceerp.backend.dto;

import com.riceerp.backend.entity.DeliveryItem;
import com.riceerp.backend.enums.*;
import java.time.*;
import java.util.List;

/** Explicit public contract. Persistence metadata and back references are not exposed. */
public record DeliveryItemResponse(
        Long id,
        ProductResponse product,
        int orderedQuantity,
        int deliveringQuantity,
        int deliveredQuantity,
        java.math.BigDecimal unitPrice) {
    public static DeliveryItemResponse from(DeliveryItem e) {
        if (e == null) return null;
        return new DeliveryItemResponse(
                e.getId(),
                ProductResponse.from(e.getProduct()),
                e.getOrderedQuantity(),
                e.getDeliveringQuantity(),
                e.getDeliveredQuantity(),
                e.getUnitPrice());
    }
}

