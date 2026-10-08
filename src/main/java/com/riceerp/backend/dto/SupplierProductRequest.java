package com.riceerp.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public class SupplierProductRequest {
    @NotNull(message = "Product is required")
    private Long productId;

    @NotNull(message = "Purchase price is required")
    @Positive(message = "Purchase price must be greater than zero")
    @jakarta.validation.constraints.Digits(integer = 15, fraction = 4, message = "Purchase price must have at most 15 integer digits and four decimal places")
    private java.math.BigDecimal purchasePrice;

    @PositiveOrZero(message = "Lead time must be zero or greater")
    private Integer leadTimeDays;

    @NotNull(message = "Minimum order quantity is required")
    @Positive(message = "Minimum order quantity must be greater than zero")
    private double minOrderQty;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public java.math.BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(double purchasePrice) { this.purchasePrice = java.math.BigDecimal.valueOf(purchasePrice); }
    @com.fasterxml.jackson.annotation.JsonSetter("purchasePrice")
    public void setPurchasePrice(java.math.BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice == null ? null : purchasePrice.stripTrailingZeros();
    }

    public Integer getLeadTimeDays() {
        return leadTimeDays;
    }

    public void setLeadTimeDays(Integer leadTimeDays) {
        this.leadTimeDays = leadTimeDays;
    }

    public double getMinOrderQty() {
        return minOrderQty;
    }

    public void setMinOrderQty(double minOrderQty) {
        this.minOrderQty = minOrderQty;
    }
}
