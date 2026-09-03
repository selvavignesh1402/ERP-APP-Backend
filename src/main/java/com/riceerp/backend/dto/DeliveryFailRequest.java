package com.riceerp.backend.dto;

import com.riceerp.backend.enums.DeliveryFailureReason;
import jakarta.validation.constraints.NotNull;

public class DeliveryFailRequest {

    @NotNull(message = "Failure reason is required")
    private DeliveryFailureReason failureReason;

    private String deliveryNotes;

    public DeliveryFailureReason getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(DeliveryFailureReason failureReason) {
        this.failureReason = failureReason;
    }

    public String getDeliveryNotes() {
        return deliveryNotes;
    }

    public void setDeliveryNotes(String deliveryNotes) {
        this.deliveryNotes = deliveryNotes;
    }
}
