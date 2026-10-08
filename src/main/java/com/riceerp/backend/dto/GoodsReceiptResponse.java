package com.riceerp.backend.dto;

import com.riceerp.backend.entity.GoodsReceipt;
import com.riceerp.backend.enums.*;
import java.time.*;
import java.util.List;

/** Explicit public contract. Persistence metadata and back references are not exposed. */
public record GoodsReceiptResponse(
        Long id,
        PurchaseResponse purchase,
        String receiptNumber,
        LocalDateTime receivedDate,
        LocalDateTime createdAt,
        Long organizationId) {
    public static GoodsReceiptResponse from(GoodsReceipt e) {
        if (e == null) return null;
        return new GoodsReceiptResponse(
                e.getId(),
                PurchaseResponse.from(e.getPurchase()),
                e.getReceiptNumber(),
                e.getReceivedDate(),
                e.getCreatedAt(),
                e.getOrganizationId());
    }
}

