package com.riceerp.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class StockAdjustmentRequest {
    @NotNull(message = "Product is required")
    private Long productId;

    @NotNull(message = "Quantity change is required")
    private double quantityChange;

    @NotBlank(message = "Reason is required")
    private String reason;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public double getQuantityChange() {
        return quantityChange;
    }

    public void setQuantityChange(double quantityChange) {
        this.quantityChange = quantityChange;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
