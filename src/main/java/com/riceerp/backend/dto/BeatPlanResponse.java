package com.riceerp.backend.dto;

import com.riceerp.backend.entity.BeatPlan;
import com.riceerp.backend.enums.*;
import java.time.*;
import java.util.List;

/** Explicit public contract. Persistence metadata and back references are not exposed. */
public record BeatPlanResponse(
        Long id,
        String name,
        UserResponse salesperson,
        boolean active,
        LocalDateTime createdAt,
        List<BeatPlanEntryResponse> entries,
        Long organizationId) {
    public static BeatPlanResponse from(BeatPlan e) {
        if (e == null) return null;
        return new BeatPlanResponse(
                e.getId(),
                e.getName(),
                UserResponse.from(e.getSalesperson()),
                e.isActive(),
                e.getCreatedAt(),
                e.getEntries() == null ? List.of() : e.getEntries().stream().map(BeatPlanEntryResponse::from).toList(),
                e.getOrganizationId());
    }
}

