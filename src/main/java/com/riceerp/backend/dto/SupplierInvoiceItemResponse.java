package com.riceerp.backend.dto;

import com.riceerp.backend.entity.SupplierInvoiceItem;
import com.riceerp.backend.enums.*;
import java.time.*;
import java.util.List;

/** Explicit public contract. Persistence metadata and back references are not exposed. */
public record SupplierInvoiceItemResponse(
        Long id,
        ProductResponse product,
        double quantity,
        java.math.BigDecimal unitPrice,
        java.math.BigDecimal totalAmount,
        Long organizationId) {
    public static SupplierInvoiceItemResponse from(SupplierInvoiceItem e) {
        if (e == null) return null;
        return new SupplierInvoiceItemResponse(
                e.getId(),
                ProductResponse.from(e.getProduct()),
                e.getQuantity(),
                e.getUnitPrice(),
                e.getTotalAmount(),
                e.getOrganizationId());
    }
}

