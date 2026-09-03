package com.riceerp.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class DeliveryItemCreateRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @Min(value = 1, message = "Delivering quantity must be at least 1")
    private int deliveringQuantity;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public int getDeliveringQuantity() {
        return deliveringQuantity;
    }

    public void setDeliveringQuantity(int deliveringQuantity) {
        this.deliveringQuantity = deliveringQuantity;
    }
}
