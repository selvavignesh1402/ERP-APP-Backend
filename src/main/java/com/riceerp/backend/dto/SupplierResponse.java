package com.riceerp.backend.dto;

import com.riceerp.backend.entity.Supplier;
import com.riceerp.backend.enums.*;
import java.time.*;
import java.util.List;

/** Explicit public contract. Persistence metadata and back references are not exposed. */
public record SupplierResponse(
        Long id,
        String supplierName,
        String phone,
        String email,
        String address,
        String gstNumber,
        double rating,
        String category,
        Status status,
        LocalDateTime createdAt,
        Long organizationId) {
    public static SupplierResponse from(Supplier e) {
        if (e == null) return null;
        return new SupplierResponse(
                e.getId(),
                e.getSupplierName(),
                e.getPhone(),
                e.getEmail(),
                e.getAddress(),
                e.getGstNumber(),
                e.getRating(),
                e.getCategory(),
                e.getStatus(),
                e.getCreatedAt(),
                e.getOrganizationId());
    }
}

