package com.riceerp.backend.dto;

public class SupplierOptionResponse {
    private Long supplierId;
    private String supplierName;
    private java.math.BigDecimal purchasePrice = java.math.BigDecimal.ZERO;
    private Integer leadTimeDays;
    private double minOrderQty;

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public java.math.BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(double purchasePrice) { this.purchasePrice = java.math.BigDecimal.valueOf(purchasePrice); }
    public void setPurchasePrice(java.math.BigDecimal purchasePrice) {
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