package com.riceerp.backend.entity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import org.hibernate.annotations.TenantId;
import com.riceerp.backend.enums.PriceType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_history")
public class PriceHistory {

    @TenantId
    @Column(name = "organization_id")
    private Long organizationId;


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "price_type", nullable = false)
    private PriceType priceType;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(nullable = false, columnDefinition = "decimal(19,4)")
    private java.math.BigDecimal price = java.math.BigDecimal.ZERO;

    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom = LocalDateTime.now();

    public PriceHistory() {
    }

    public PriceHistory(Product product, PriceType priceType, java.math.BigDecimal price) {
        this.product = product;
        this.priceType = priceType;
        this.price = price;
        this.effectiveFrom = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public PriceType getPriceType() {
        return priceType;
    }

    public void setPriceType(PriceType priceType) {
        this.priceType = priceType;
    }

    public java.math.BigDecimal getPrice() {
        return price;
    }

    public void setPrice(double price) { this.price = java.math.BigDecimal.valueOf(price); }
    public void setPrice(java.math.BigDecimal price) {
        this.price = price;
    }

    public LocalDateTime getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDateTime effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

}