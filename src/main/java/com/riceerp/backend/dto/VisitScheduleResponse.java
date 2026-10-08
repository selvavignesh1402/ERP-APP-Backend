package com.riceerp.backend.dto;

import com.riceerp.backend.entity.VisitSchedule;
import com.riceerp.backend.enums.*;
import java.time.*;
import java.util.List;

/** Explicit public contract. Persistence metadata and back references are not exposed. */
public record VisitScheduleResponse(
        Long id,
        BeatPlanResponse beatPlan,
        UserResponse salesperson,
        CustomerResponse customer,
        LocalDate scheduledDate,
        int visitOrder,
        VisitStatus status,
        Long organizationId) {
    public static VisitScheduleResponse from(VisitSchedule e) {
        if (e == null) return null;
        return new VisitScheduleResponse(
                e.getId(),
                BeatPlanResponse.from(e.getBeatPlan()),
                UserResponse.from(e.getSalesperson()),
                CustomerResponse.from(e.getCustomer()),
                e.getScheduledDate(),
                e.getVisitOrder(),
                e.getStatus(),
                e.getOrganizationId());
    }
}

