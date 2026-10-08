package com.riceerp.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class PurchaseItemRequest {
    @NotNull(message = "Product is required")
    private Long productId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    private double quantity;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than zero")
    @jakarta.validation.constraints.Digits(integer = 15, fraction = 4)
    private java.math.BigDecimal price;

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

    @com.fasterxml.jackson.annotation.JsonSetter("price")
    public void setPrice(java.math.BigDecimal price) {
        this.price = price == null ? null : price.stripTrailingZeros();
    }
    public void setPrice(double price) {
        this.price = java.math.BigDecimal.valueOf(price);
    }
}
