package com.riceerp.backend.dto;

import com.riceerp.backend.entity.ReconciliationResult;
import com.riceerp.backend.enums.*;
import java.time.*;
import java.util.List;

/** Explicit public contract. Persistence metadata and back references are not exposed. */
public record ReconciliationResultResponse(
        Long id,
        PurchaseResponse purchase,
        SupplierInvoiceResponse invoice,
        ReconciliationStatus status,
        java.math.BigDecimal amountMatched,
        java.math.BigDecimal amountOnPurchase,
        java.math.BigDecimal amountOnInvoice,
        String details,
        LocalDateTime reconciledAt,
        Long organizationId) {
    public static ReconciliationResultResponse from(ReconciliationResult e) {
        if (e == null) return null;
        return new ReconciliationResultResponse(
                e.getId(),
                PurchaseResponse.from(e.getPurchase()),
                SupplierInvoiceResponse.from(e.getInvoice()),
                e.getStatus(),
                e.getAmountMatched(),
                e.getAmountOnPurchase(),
                e.getAmountOnInvoice(),
                e.getDetails(),
                e.getReconciledAt(),
                e.getOrganizationId());
    }
}

