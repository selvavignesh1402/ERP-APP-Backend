package com.riceerp.backend.dto;

import com.riceerp.backend.entity.StockAdjustment;
import com.riceerp.backend.enums.*;
import java.time.*;
import java.util.List;

/** Explicit public contract. Persistence metadata and back references are not exposed. */
public record StockAdjustmentResponse(
        Long id,
        ProductResponse product,
        double quantityChange,
        String reason,
        LocalDateTime adjustedAt,
        Long organizationId) {
    public static StockAdjustmentResponse from(StockAdjustment e) {
        if (e == null) return null;
        return new StockAdjustmentResponse(
                e.getId(),
                ProductResponse.from(e.getProduct()),
                e.getQuantityChange(),
                e.getReason(),
                e.getAdjustedAt(),
                e.getOrganizationId());
    }
}

