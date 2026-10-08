package com.riceerp.backend.dto;

import com.riceerp.backend.entity.Product;
import com.riceerp.backend.enums.*;
import java.time.*;
import java.util.List;

/** Explicit public contract. Persistence metadata and back references are not exposed. */
public record ProductResponse(
        Double reservedStock,
        Double availableStock,
        Long id,
        String productName,
        String category,
        String brand,
        String unit,
        java.math.BigDecimal purchasePrice,
        java.math.BigDecimal sellingPrice,
        double stock,
        double minimumStock,
        Double gstRate,
        String hsnCode,
        Status status,
        LocalDateTime createdAt,
        Long organizationId) {
    public static ProductResponse from(Product e) {
        if (e == null) return null;
        return new ProductResponse(
                e.getReservedStock(),
                e.getAvailableStock(),
                e.getId(),
                e.getProductName(),
                e.getCategory(),
                e.getBrand(),
                e.getUnit(),
                e.getPurchasePrice(),
                e.getSellingPrice(),
                e.getStock(),
                e.getMinimumStock(),
                e.getGstRate(),
                e.getHsnCode(),
                e.getStatus(),
                e.getCreatedAt(),
                e.getOrganizationId());
    }
}

