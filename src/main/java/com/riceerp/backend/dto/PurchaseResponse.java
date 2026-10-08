package com.riceerp.backend.dto;

import com.riceerp.backend.entity.Purchase;
import com.riceerp.backend.enums.*;
import java.time.*;
import java.util.List;

/** Explicit public contract. Persistence metadata and back references are not exposed. */
public record PurchaseResponse(
        Long id,
        SupplierResponse supplier,
        String invoiceNumber,
        LocalDateTime purchaseDate,
        java.math.BigDecimal totalAmount,
        PurchaseStatus status,
        LocalDateTime createdAt,
        Long organizationId) {
    public static PurchaseResponse from(Purchase e) {
        if (e == null) return null;
        return new PurchaseResponse(
                e.getId(),
                SupplierResponse.from(e.getSupplier()),
                e.getInvoiceNumber(),
                e.getPurchaseDate(),
                e.getTotalAmount(),
                e.getStatus(),
                e.getCreatedAt(),
                e.getOrganizationId());
    }
}

