package com.riceerp.backend.entity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import org.hibernate.annotations.TenantId;
import com.riceerp.backend.enums.Status;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class Product {
    @Transient
    private Double reservedStock;

    public Double getReservedStock() { return reservedStock; }
    public void setReservedStock(Double reservedStock) { this.reservedStock = reservedStock; }
    /** Null means reservations have not been loaded; treating unknown as zero could oversell stock. */
    public Double getAvailableStock() { return reservedStock == null ? null : Math.max(0, stock - reservedStock); }

    @TenantId
    @Column(name = "organization_id")
    private Long organizationId;


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column
    private String category;

    @Column
    private String brand;

    @Column
    private String unit;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "purchase_price", nullable = false, columnDefinition = "decimal(19,4)")
    private double purchasePrice;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "selling_price", nullable = false, columnDefinition = "decimal(19,4)")
    private double sellingPrice;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(nullable = false, columnDefinition = "decimal(19,6)")
    private double stock = 0.0;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "minimum_stock", nullable = false, columnDefinition = "decimal(19,6)")
    private double minimumStock = 0.0;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "gst_rate", nullable = false, columnDefinition = "decimal(7,4)")
    private double gstRate = 0.0;

    @Column(name = "hsn_code")
    private String hsnCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Version
    private Long version = 0L;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public double getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(double purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public double getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(double sellingPrice) {
        this.sellingPrice = sellingPrice;
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

    public double getGstRate() {
        return gstRate;
    }

    public void setGstRate(double gstRate) {
        this.gstRate = gstRate;
    }

    public String getHsnCode() {
        return hsnCode;
    }

    public void setHsnCode(String hsnCode) {
        this.hsnCode = hsnCode;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

}
