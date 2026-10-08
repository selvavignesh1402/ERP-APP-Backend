package com.riceerp.backend.entity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import org.hibernate.annotations.TenantId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "supplier_products",
        uniqueConstraints = @UniqueConstraint(columnNames = {"supplier_id", "product_id"}))
public class SupplierProduct {

    @TenantId
    @Column(name = "organization_id")
    private Long organizationId;


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "supplier_id", nullable = false)
    @JsonIgnore
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "purchase_price", nullable = false, columnDefinition = "decimal(19,4)")
    private java.math.BigDecimal purchasePrice = java.math.BigDecimal.ZERO;

    @Column(name = "lead_time_days")
    private Integer leadTimeDays;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "min_order_qty", columnDefinition = "decimal(19,6)")
    private double minOrderQty;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
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

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

}