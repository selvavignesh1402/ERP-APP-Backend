package com.riceerp.backend.dto;

import com.riceerp.backend.entity.SaleItem;
import com.riceerp.backend.enums.*;
import java.time.*;
import java.util.List;

/** Explicit public contract. Persistence metadata and back references are not exposed. */
public record SaleItemResponse(
        String productName,
        String unit,
        Long id,
        ProductResponse product,
        Double gstRate,
        double quantity,
        java.math.BigDecimal price,
        Long organizationId) {
    public static SaleItemResponse from(SaleItem e) {
        if (e == null) return null;
        return new SaleItemResponse(
                e.getProductName(),
                e.getUnit(),
                e.getId(),
                ProductResponse.from(e.getProduct()),
                e.getGstRate(),
                e.getQuantity(),
                e.getPrice(),
                e.getOrganizationId());
    }
}

