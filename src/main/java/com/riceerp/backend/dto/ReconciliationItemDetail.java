package com.riceerp.backend.dto;

public class ReconciliationItemDetail {
    private Long productId;
    private String productName;
    private double orderedQty;
    private double receivedQty;
    private double billedQty;
    private double orderedPrice;
    private double billedPrice;
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

    public double getOrderedPrice() {
        return orderedPrice;
    }

    public void setOrderedPrice(double orderedPrice) {
        this.orderedPrice = orderedPrice;
    }

    public double getBilledPrice() {
        return billedPrice;
    }

    public void setBilledPrice(double billedPrice) {
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