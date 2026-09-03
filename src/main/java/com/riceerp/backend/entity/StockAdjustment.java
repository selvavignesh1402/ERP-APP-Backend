package com.riceerp.backend.entity;

import org.hibernate.annotations.TenantId;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_adjustments")
public class StockAdjustment {

    @TenantId
    @Column(name = "organization_id")
    private Long organizationId;


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity_change", nullable = false)
    private double quantityChange;

    @Column(nullable = false)
    private String reason; // 'damage' | 'recount' | 'spillage' | 'other'

    @Column(name = "adjusted_at", nullable = false)
    private LocalDateTime adjustedAt = LocalDateTime.now();

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

    public double getQuantityChange() {
        return quantityChange;
    }

    public void setQuantityChange(double quantityChange) {
        this.quantityChange = quantityChange;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getAdjustedAt() {
        return adjustedAt;
    }

    public void setAdjustedAt(LocalDateTime adjustedAt) {
        this.adjustedAt = adjustedAt;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

}
