package com.riceerp.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public class SupplierProductRequest {
    @NotNull(message = "Product is required")
    private Long productId;

    @NotNull(message = "Purchase price is required")
    @Positive(message = "Purchase price must be greater than zero")
    private double purchasePrice;

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

    public double getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(double purchasePrice) {
        this.purchasePrice = purchasePrice;
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