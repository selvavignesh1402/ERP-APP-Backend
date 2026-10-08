package com.riceerp.backend.dto;

import com.riceerp.backend.entity.GoodsReceiptItem;
import com.riceerp.backend.enums.*;
import java.time.*;
import java.util.List;

/** Explicit public contract. Persistence metadata and back references are not exposed. */
public record GoodsReceiptItemResponse(
        Long id,
        ProductResponse product,
        double orderedQty,
        double receivedQty,
        java.math.BigDecimal unitPrice,
        Long organizationId) {
    public static GoodsReceiptItemResponse from(GoodsReceiptItem e) {
        if (e == null) return null;
        return new GoodsReceiptItemResponse(
                e.getId(),
                ProductResponse.from(e.getProduct()),
                e.getOrderedQty(),
                e.getReceivedQty(),
                e.getUnitPrice(),
                e.getOrganizationId());
    }
}

