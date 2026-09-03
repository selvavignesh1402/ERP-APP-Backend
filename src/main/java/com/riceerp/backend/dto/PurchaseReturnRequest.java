package com.riceerp.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class PurchaseReturnRequest {
    @NotNull(message = "Product is required")
    private Long productId;

    @NotNull(message = "Quantity returned is required")
    @Positive(message = "Quantity returned must be greater than zero")
    private double quantityReturned;

    private String reason;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public double getQuantityReturned() {
        return quantityReturned;
    }

    public void setQuantityReturned(double quantityReturned) {
        this.quantityReturned = quantityReturned;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
