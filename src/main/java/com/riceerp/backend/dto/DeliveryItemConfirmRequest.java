package com.riceerp.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class DeliveryItemConfirmRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @Min(value = 0, message = "Delivered quantity cannot be negative")
    private int deliveredQuantity;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public int getDeliveredQuantity() {
        return deliveredQuantity;
    }

    public void setDeliveredQuantity(int deliveredQuantity) {
        this.deliveredQuantity = deliveredQuantity;
    }
}
