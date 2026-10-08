package com.riceerp.backend.dto;

import com.riceerp.backend.entity.PurchaseReturn;
import com.riceerp.backend.enums.*;
import java.time.*;
import java.util.List;

/** Explicit public contract. Persistence metadata and back references are not exposed. */
public record PurchaseReturnResponse(
        Long id,
        PurchaseResponse purchase,
        ProductResponse product,
        double quantityReturned,
        String reason,
        LocalDateTime returnDate,
        Long organizationId) {
    public static PurchaseReturnResponse from(PurchaseReturn e) {
        if (e == null) return null;
        return new PurchaseReturnResponse(
                e.getId(),
                PurchaseResponse.from(e.getPurchase()),
                ProductResponse.from(e.getProduct()),
                e.getQuantityReturned(),
                e.getReason(),
                e.getReturnDate(),
                e.getOrganizationId());
    }
}

