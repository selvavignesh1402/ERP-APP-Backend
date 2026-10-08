package com.riceerp.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class SalesOrderItemRequest {
    private Double gstRate;
    private boolean gstRateProvided;

    public Double getGstRate() { return gstRate; }
    public void setGstRate(Double value) { gstRate = value; gstRateProvided = true; }
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isGstRateProvided() { return gstRateProvided; }

    @NotNull(message = "Product ID is required")
    private Long productId;

    @Min(value = 1, message = "Ordered quantity must be at least 1")
    private int quantity;

    @Min(value = 0, message = "Unit price cannot be negative")
    @NotNull(message = "Unit price cannot be null")
    @jakarta.validation.constraints.Digits(integer = 15, fraction = 2, message = "Unit price must have at most 15 integer digits and two decimal places")
    private java.math.BigDecimal unitPrice = java.math.BigDecimal.ZERO;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public java.math.BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) { this.unitPrice = java.math.BigDecimal.valueOf(unitPrice); }
    @com.fasterxml.jackson.annotation.JsonSetter("unitPrice")
    public void setUnitPrice(java.math.BigDecimal unitPrice) {
        this.unitPrice = unitPrice == null ? null : unitPrice.stripTrailingZeros();
    }
}
