package com.riceerp.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class SaleItemRequest {
    private Double gstRate;
    private boolean gstRateProvided;
    public Double getGstRate() { return gstRate; }
    public void setGstRate(Double value) { gstRate = value; gstRateProvided = true; }
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isGstRateProvided() { return gstRateProvided; }
    @NotNull(message = "Product is required")
    private Long productId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    private double quantity;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than zero")
    @jakarta.validation.constraints.Digits(integer = 15, fraction = 4, message = "Price must have at most 15 integer digits and four decimal places")
    private java.math.BigDecimal price = java.math.BigDecimal.ZERO;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public java.math.BigDecimal getPrice() {
        return price;
    }

    public void setPrice(double price) { this.price = java.math.BigDecimal.valueOf(price); }
    @com.fasterxml.jackson.annotation.JsonSetter("price")
    public void setPrice(java.math.BigDecimal price) {
        this.price = price == null ? null : price.stripTrailingZeros();
    }
}
