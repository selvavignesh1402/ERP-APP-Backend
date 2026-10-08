package com.riceerp.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;

public class PaymentRequest {
    @jakarta.validation.constraints.NotBlank(message = "Payment request ID is required")
    @jakarta.validation.constraints.Size(max = 64)
    private String clientReferenceId;
    public String getClientReferenceId() { return clientReferenceId; }
    public void setClientReferenceId(String id) { clientReferenceId = id; }
    @NotNull(message = "Reference type is required")
    private String referenceType;

    @NotNull(message = "Reference id is required")
    private Long referenceId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Payment amount must be greater than zero")
    @Digits(integer = 15, fraction = 2, message = "Payment amount must have at most 15 integer digits and two decimal places")
    private BigDecimal amount;

    @NotNull(message = "Payment mode is required")
    private String paymentMode;

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Long referenceId) {
        this.referenceId = referenceId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        // Trailing zeroes do not change monetary precision (for example, 10.000 = 10).
        this.amount = amount == null ? null : amount.stripTrailingZeros();
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }
}
