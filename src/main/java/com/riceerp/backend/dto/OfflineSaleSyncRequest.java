package com.riceerp.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

public class OfflineSaleSyncRequest {
    private com.riceerp.backend.enums.TaxType taxType = com.riceerp.backend.enums.TaxType.INTRA_STATE;
    public com.riceerp.backend.enums.TaxType getTaxType() { return taxType; }
    public void setTaxType(com.riceerp.backend.enums.TaxType value) { taxType = value; }

    @NotBlank(message = "clientReferenceId is required for offline sync idempotency")
    private String clientReferenceId;

    private String customerName;
    private Long customerId;

    @NotBlank(message = "Payment mode is required")
    private String paymentMode;
    @PositiveOrZero(message = "Paid amount must be zero or greater")
    @jakarta.validation.constraints.Digits(integer = 15, fraction = 2, message = "Paid amount must have at most 15 integer digits and two decimal places")
    private BigDecimal paidAmount;
    private String initialPaymentMode;

    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount == null ? null : paidAmount.stripTrailingZeros(); }
    public String getInitialPaymentMode() { return initialPaymentMode; }
    public void setInitialPaymentMode(String mode) { this.initialPaymentMode = mode; }

    @NotNull(message = "Discount is required")
    @PositiveOrZero(message = "Discount must be zero or greater")
    @jakarta.validation.constraints.Digits(integer = 15, fraction = 4, message = "Discount must have at most 15 integer digits and four decimal places")
    private BigDecimal discount = BigDecimal.ZERO;

    private LocalDateTime offlineCreatedAt;

    @NotEmpty(message = "Sale must contain at least one item")
    @Valid
    private List<SaleItemRequest> items;

    public String getClientReferenceId() {
        return clientReferenceId;
    }

    public void setClientReferenceId(String clientReferenceId) {
        this.clientReferenceId = clientReferenceId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount == null ? null : discount.stripTrailingZeros();
    }

    public LocalDateTime getOfflineCreatedAt() {
        return offlineCreatedAt;
    }

    public void setOfflineCreatedAt(LocalDateTime offlineCreatedAt) {
        this.offlineCreatedAt = offlineCreatedAt;
    }

    public List<SaleItemRequest> getItems() {
        return items;
    }

    public void setItems(List<SaleItemRequest> items) {
        this.items = items;
    }
}
