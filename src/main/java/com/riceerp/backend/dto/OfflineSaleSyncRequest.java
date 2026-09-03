package com.riceerp.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDateTime;
import java.util.List;

public class OfflineSaleSyncRequest {

    @NotBlank(message = "clientReferenceId is required for offline sync idempotency")
    private String clientReferenceId;

    private String customerName;
    private Long customerId;

    @NotBlank(message = "Payment mode is required")
    private String paymentMode;

    @NotNull(message = "Discount is required")
    @PositiveOrZero(message = "Discount must be zero or greater")
    private double discount;

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

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
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
