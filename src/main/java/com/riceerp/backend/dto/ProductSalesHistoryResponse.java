package com.riceerp.backend.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ProductSalesHistoryResponse {

    public static class DailyBreakdown {
        private LocalDate date;
        private double quantity;
        private double revenue;

        public DailyBreakdown() {
        }

        public DailyBreakdown(LocalDate date, double quantity, double revenue) {
            this.date = date;
            this.quantity = quantity;
            this.revenue = revenue;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public double getQuantity() {
            return quantity;
        }

        public void setQuantity(double quantity) {
            this.quantity = quantity;
        }

        public double getRevenue() {
            return revenue;
        }

        public void setRevenue(double revenue) {
            this.revenue = revenue;
        }
    }

    private Long productId;
    private String productName;
    private String unit;
    private LocalDate startDate;
    private LocalDate endDate;
    private double totalQuantitySold;
    private double totalRevenue;
    private long salesCount;
    private double averageDailySales;
    private List<DailyBreakdown> breakdown = new ArrayList<>();

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

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public double getTotalQuantitySold() {
        return totalQuantitySold;
    }

    public void setTotalQuantitySold(double totalQuantitySold) {
        this.totalQuantitySold = totalQuantitySold;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public long getSalesCount() {
        return salesCount;
    }

    public void setSalesCount(long salesCount) {
        this.salesCount = salesCount;
    }

    public double getAverageDailySales() {
        return averageDailySales;
    }

    public void setAverageDailySales(double averageDailySales) {
        this.averageDailySales = averageDailySales;
    }

    public List<DailyBreakdown> getBreakdown() {
        return breakdown;
    }

    public void setBreakdown(List<DailyBreakdown> breakdown) {
        this.breakdown = breakdown;
    }
}