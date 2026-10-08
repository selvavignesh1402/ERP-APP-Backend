package com.riceerp.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public class ProductRequest {
    @NotBlank(message = "Product name is required")
    private String productName;

    @NotBlank(message = "Category is required")
    private String category;

    private String brand;
    private String unit;

    @PositiveOrZero(message = "Purchase price must be zero or greater")
    @jakarta.validation.constraints.NotNull(message = "Purchase price cannot be null")
    @jakarta.validation.constraints.Digits(integer = 15, fraction = 4, message = "Purchase price must have at most 15 integer digits and four decimal places")
    private java.math.BigDecimal purchasePrice = java.math.BigDecimal.ZERO;

    @Positive(message = "Selling price must be greater than zero")
    @jakarta.validation.constraints.NotNull(message = "Selling price cannot be null")
    @jakarta.validation.constraints.Digits(integer = 15, fraction = 4, message = "Selling price must have at most 15 integer digits and four decimal places")
    private java.math.BigDecimal sellingPrice = java.math.BigDecimal.ZERO;

    @PositiveOrZero(message = "Stock must be zero or greater")
    private double stock;

    @PositiveOrZero(message = "Minimum stock must be zero or greater")
    private double minimumStock;

    @PositiveOrZero(message = "GST rate must be zero or greater")
    private Double gstRate;

    private String hsnCode;

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public java.math.BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(double purchasePrice) { this.purchasePrice = java.math.BigDecimal.valueOf(purchasePrice); }
    @com.fasterxml.jackson.annotation.JsonSetter("purchasePrice")
    public void setPurchasePrice(java.math.BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice == null ? null : purchasePrice.stripTrailingZeros();
    }

    public java.math.BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(double sellingPrice) { this.sellingPrice = java.math.BigDecimal.valueOf(sellingPrice); }
    @com.fasterxml.jackson.annotation.JsonSetter("sellingPrice")
    public void setSellingPrice(java.math.BigDecimal sellingPrice) {
        this.sellingPrice = sellingPrice == null ? null : sellingPrice.stripTrailingZeros();
    }

    public double getStock() {
        return stock;
    }

    public void setStock(double stock) {
        this.stock = stock;
    }

    public double getMinimumStock() {
        return minimumStock;
    }

    public void setMinimumStock(double minimumStock) {
        this.minimumStock = minimumStock;
    }

    public Double getGstRate() {
        return gstRate;
    }

    public void setGstRate(Double gstRate) {
        this.gstRate = gstRate;
    }

    public String getHsnCode() {
        return hsnCode;
    }

    public void setHsnCode(String hsnCode) {
        this.hsnCode = hsnCode;
    }
}
