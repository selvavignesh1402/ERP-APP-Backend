package com.riceerp.backend.dto;

import com.riceerp.backend.entity.VisitCheckIn;
import com.riceerp.backend.enums.*;
import java.time.*;
import java.util.List;

/** Explicit public contract. Persistence metadata and back references are not exposed. */
public record VisitCheckInResponse(
        Long id,
        VisitScheduleResponse visitSchedule,
        UserResponse salesperson,
        CustomerResponse customer,
        LocalDateTime checkInTime,
        LocalDateTime checkOutTime,
        Double latitude,
        Double longitude,
        VisitOutcome outcome,
        String notes,
        Long saleId,
        Long paymentId,
        Long organizationId) {
    public static VisitCheckInResponse from(VisitCheckIn e) {
        if (e == null) return null;
        return new VisitCheckInResponse(
                e.getId(),
                VisitScheduleResponse.from(e.getVisitSchedule()),
                UserResponse.from(e.getSalesperson()),
                CustomerResponse.from(e.getCustomer()),
                e.getCheckInTime(),
                e.getCheckOutTime(),
                e.getLatitude(),
                e.getLongitude(),
                e.getOutcome(),
                e.getNotes(),
                e.getSaleId(),
                e.getPaymentId(),
                e.getOrganizationId());
    }
}

