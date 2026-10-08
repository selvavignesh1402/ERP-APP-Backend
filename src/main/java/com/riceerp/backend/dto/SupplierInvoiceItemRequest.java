package com.riceerp.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class SupplierInvoiceItemRequest {
    @NotNull(message = "Product is required")
    private Long productId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    private double quantity;

    @NotNull(message = "Unit price is required")
    @Positive(message = "Unit price must be greater than zero")
    @jakarta.validation.constraints.Digits(integer = 15, fraction = 4)
    private java.math.BigDecimal unitPrice;

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

    public java.math.BigDecimal getUnitPrice() {
        return unitPrice;
    }

    @com.fasterxml.jackson.annotation.JsonSetter("unitPrice")
    public void setUnitPrice(java.math.BigDecimal unitPrice) {
        this.unitPrice = unitPrice == null ? null : unitPrice.stripTrailingZeros();
    }
    public void setUnitPrice(double unitPrice) {
        this.unitPrice = java.math.BigDecimal.valueOf(unitPrice);
    }
}