package com.riceerp.backend.dto;

import com.riceerp.backend.entity.BeatPlanEntry;
import com.riceerp.backend.enums.*;
import java.time.*;
import java.util.List;

/** Explicit public contract. Persistence metadata and back references are not exposed. */
public record BeatPlanEntryResponse(
        Long id,
        DayOfWeek dayOfWeek,
        CustomerResponse customer,
        int visitOrder,
        Long organizationId) {
    public static BeatPlanEntryResponse from(BeatPlanEntry e) {
        if (e == null) return null;
        return new BeatPlanEntryResponse(
                e.getId(),
                e.getDayOfWeek(),
                CustomerResponse.from(e.getCustomer()),
                e.getVisitOrder(),
                e.getOrganizationId());
    }
}

