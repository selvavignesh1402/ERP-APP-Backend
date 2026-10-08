package com.riceerp.backend.dto;

public class ReconciliationItemDetail {
    private double previouslyBilledQty;
    private double availableReceivedQty;
    private java.math.BigDecimal orderedAmount = java.math.BigDecimal.ZERO;
    private java.math.BigDecimal billedAmount = java.math.BigDecimal.ZERO;
    public double getPreviouslyBilledQty() { return previouslyBilledQty; }
    public void setPreviouslyBilledQty(double value) { previouslyBilledQty = value; }
    public double getAvailableReceivedQty() { return availableReceivedQty; }
    public void setAvailableReceivedQty(double value) { availableReceivedQty = value; }
    public java.math.BigDecimal getOrderedAmount() { return orderedAmount; }
    public void setOrderedAmount(java.math.BigDecimal value) { orderedAmount = value; }
    public java.math.BigDecimal getBilledAmount() { return billedAmount; }
    public void setBilledAmount(java.math.BigDecimal value) { billedAmount = value; }
    private Long productId;
    private String productName;
    private double orderedQty;
    private double receivedQty;
    private double billedQty;
    private java.math.BigDecimal orderedPrice = java.math.BigDecimal.ZERO;
    private java.math.BigDecimal billedPrice = java.math.BigDecimal.ZERO;
    private boolean qtyMatch;
    private boolean priceMatch;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public double getOrderedQty() {
        return orderedQty;
    }

    public void setOrderedQty(double orderedQty) {
        this.orderedQty = orderedQty;
    }

    public double getReceivedQty() {
        return receivedQty;
    }

    public void setReceivedQty(double receivedQty) {
        this.receivedQty = receivedQty;
    }

    public double getBilledQty() {
        return billedQty;
    }

    public void setBilledQty(double billedQty) {
        this.billedQty = billedQty;
    }

    public java.math.BigDecimal getOrderedPrice() {
        return orderedPrice;
    }

    public void setOrderedPrice(java.math.BigDecimal orderedPrice) {
        this.orderedPrice = orderedPrice;
    }

    public java.math.BigDecimal getBilledPrice() {
        return billedPrice;
    }

    public void setBilledPrice(java.math.BigDecimal billedPrice) {
        this.billedPrice = billedPrice;
    }

    public boolean isQtyMatch() {
        return qtyMatch;
    }

    public void setQtyMatch(boolean qtyMatch) {
        this.qtyMatch = qtyMatch;
    }

    public boolean isPriceMatch() {
        return priceMatch;
    }

    public void setPriceMatch(boolean priceMatch) {
        this.priceMatch = priceMatch;
    }
}
