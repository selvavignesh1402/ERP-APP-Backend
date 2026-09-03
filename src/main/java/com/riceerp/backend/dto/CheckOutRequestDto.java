package com.riceerp.backend.dto;

import com.riceerp.backend.enums.VisitOutcome;
import jakarta.validation.constraints.NotNull;

public class CheckOutRequestDto {
    @NotNull(message = "Visit outcome is required")
    private VisitOutcome outcome;

    private String notes;
    private Long saleId;
    private Long paymentId;

    public VisitOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(VisitOutcome outcome) {
        this.outcome = outcome;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getSaleId() {
        return saleId;
    }

    public void setSaleId(Long saleId) {
        this.saleId = saleId;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }
}
