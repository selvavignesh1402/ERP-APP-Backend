package com.riceerp.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class GoodsReceiptItemRequest {
    @NotNull(message = "Product is required")
    private Long productId;

    @NotNull(message = "Received quantity is required")
    @Positive(message = "Received quantity must be greater than zero")
    private double receivedQty;

    @NotNull(message = "Unit price is required")
    @Positive(message = "Unit price must be greater than zero")
    private double unitPrice;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public double getReceivedQty() {
        return receivedQty;
    }

    public void setReceivedQty(double receivedQty) {
        this.receivedQty = receivedQty;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }
}