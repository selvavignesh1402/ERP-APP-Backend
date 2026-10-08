package com.riceerp.backend.dto;

import com.riceerp.backend.entity.StockMovement;
import com.riceerp.backend.enums.*;
import java.time.*;
import java.util.List;

/** Explicit public contract. Persistence metadata and back references are not exposed. */
public record StockMovementResponse(
        Long id,
        ProductResponse product,
        MovementType movementType,
        double quantity,
        Long referenceId,
        LocalDateTime createdAt,
        Long organizationId) {
    public static StockMovementResponse from(StockMovement e) {
        if (e == null) return null;
        return new StockMovementResponse(
                e.getId(),
                ProductResponse.from(e.getProduct()),
                e.getMovementType(),
                e.getQuantity(),
                e.getReferenceId(),
                e.getCreatedAt(),
                e.getOrganizationId());
    }
}

