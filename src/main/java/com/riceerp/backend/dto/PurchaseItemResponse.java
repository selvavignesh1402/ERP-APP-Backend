package com.riceerp.backend.dto;

import com.riceerp.backend.entity.PurchaseItem;
import com.riceerp.backend.enums.*;
import java.time.*;
import java.util.List;

/** Explicit public contract. Persistence metadata and back references are not exposed. */
public record PurchaseItemResponse(
        Long id,
        ProductResponse product,
        double quantity,
        java.math.BigDecimal price,
        Long organizationId) {
    public static PurchaseItemResponse from(PurchaseItem e) {
        if (e == null) return null;
        return new PurchaseItemResponse(
                e.getId(),
                ProductResponse.from(e.getProduct()),
                e.getQuantity(),
                e.getPrice(),
                e.getOrganizationId());
    }
}

