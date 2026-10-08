package com.riceerp.backend.dto;

import com.riceerp.backend.entity.Delivery;
import com.riceerp.backend.enums.*;
import java.time.*;
import java.util.List;

/** Explicit public contract. Persistence metadata and back references are not exposed. */
public record DeliveryResponse(
        Long id,
        String deliveryNumber,
        SalesOrderResponse salesOrder,
        UserResponse deliveryPerson,
        String vehicleNumber,
        DeliveryStatus status,
        LocalDateTime assignedAt,
        LocalDateTime startedAt,
        LocalDateTime deliveredAt,
        String receiverName,
        String receiverPhone,
        String deliveryNotes,
        DeliveryFailureReason failureReason,
        List<DeliveryItemResponse> items,
        Long generatedInvoiceId,
        Long organizationId) {
    public static DeliveryResponse from(Delivery e) {
        if (e == null) return null;
        return new DeliveryResponse(
                e.getId(),
                e.getDeliveryNumber(),
                SalesOrderResponse.from(e.getSalesOrder()),
                UserResponse.from(e.getDeliveryPerson()),
                e.getVehicleNumber(),
                e.getStatus(),
                e.getAssignedAt(),
                e.getStartedAt(),
                e.getDeliveredAt(),
                e.getReceiverName(),
                e.getReceiverPhone(),
                e.getDeliveryNotes(),
                e.getFailureReason(),
                e.getItems() == null ? List.of() : e.getItems().stream().map(DeliveryItemResponse::from).toList(),
                e.getGeneratedInvoiceId(),
                e.getOrganizationId());
    }
}

