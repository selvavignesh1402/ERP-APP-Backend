package com.riceerp.backend.dto;

import com.riceerp.backend.entity.SupplierProduct;
import com.riceerp.backend.enums.*;
import java.time.*;
import java.util.List;

/** Explicit public contract. Persistence metadata and back references are not exposed. */
public record SupplierProductResponse(
        Long id,
        ProductResponse product,
        java.math.BigDecimal purchasePrice,
        Integer leadTimeDays,
        double minOrderQty,
        Long organizationId) {
    public static SupplierProductResponse from(SupplierProduct e) {
        if (e == null) return null;
        return new SupplierProductResponse(
                e.getId(),
                ProductResponse.from(e.getProduct()),
                e.getPurchasePrice(),
                e.getLeadTimeDays(),
                e.getMinOrderQty(),
                e.getOrganizationId());
    }
}

