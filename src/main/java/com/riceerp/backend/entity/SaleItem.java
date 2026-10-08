package com.riceerp.backend.entity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import org.hibernate.annotations.TenantId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "sales_items")
public class SaleItem {
    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "gst_rate", columnDefinition = "decimal(7,4) default 5", updatable = false)
    private Double gstRate;
    public Double getGstRate() { return gstRate; }
    public void setGstRate(Double value) { gstRate = value; }

    @TenantId
    @Column(name = "organization_id")
    private Long organizationId;


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false)
    @JsonIgnore
    private Sale sale;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(nullable = false, columnDefinition = "decimal(19,6)")
    private double quantity;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(nullable = false, columnDefinition = "decimal(19,4)")
    private java.math.BigDecimal price = java.math.BigDecimal.ZERO;

    @Column(name = "product_name", updatable = false)
    private String productName;
    @Column(name = "unit", updatable = false)
    private String unit;
    public String getProductName() { return productName; }
    public void setProductName(String value) { productName = value; }
    public String getUnit() { return unit; }
    public void setUnit(String value) { unit = value; }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Sale getSale() {
        return sale;
    }

    public void setSale(Sale sale) {
        this.sale = sale;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
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

    public void setPrice(double price) { this.price = java.math.BigDecimal.valueOf(price); }
    public void setPrice(java.math.BigDecimal price) {
        this.price = price;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

}
