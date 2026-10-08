package com.riceerp.backend.dto;

import com.riceerp.backend.entity.SupplierInvoice;
import com.riceerp.backend.enums.*;
import java.time.*;
import java.util.List;

/** Explicit public contract. Persistence metadata and back references are not exposed. */
public record SupplierInvoiceResponse(
        Long id,
        String invoiceNumber,
        SupplierResponse supplier,
        PurchaseResponse purchase,
        LocalDateTime invoiceDate,
        java.math.BigDecimal totalAmount,
        InvoiceStatus status,
        LocalDateTime createdAt,
        Long organizationId) {
    public static SupplierInvoiceResponse from(SupplierInvoice e) {
        if (e == null) return null;
        return new SupplierInvoiceResponse(
                e.getId(),
                e.getInvoiceNumber(),
                SupplierResponse.from(e.getSupplier()),
                PurchaseResponse.from(e.getPurchase()),
                e.getInvoiceDate(),
                e.getTotalAmount(),
                e.getStatus(),
                e.getCreatedAt(),
                e.getOrganizationId());
    }
}

