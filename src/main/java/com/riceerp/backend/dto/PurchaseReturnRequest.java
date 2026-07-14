package com.riceerp.backend.dto;

public class PurchaseReturnRequest {
    private Long productId;
    private double quantityReturned;
    private String reason;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public double getQuantityReturned() {
        return quantityReturned;
    }

    public void setQuantityReturned(double quantityReturned) {
        this.quantityReturned = quantityReturned;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
