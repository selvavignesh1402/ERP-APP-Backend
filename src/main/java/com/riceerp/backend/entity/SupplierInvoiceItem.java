package com.riceerp.backend.entity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import org.hibernate.annotations.TenantId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "supplier_invoice_items")
public class SupplierInvoiceItem {

    @TenantId
    @Column(name = "organization_id")
    private Long organizationId;


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    @JsonIgnore
    private SupplierInvoice invoice;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(nullable = false, columnDefinition = "decimal(19,6)")
    private double quantity;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "unit_price", nullable = false, columnDefinition = "decimal(19,4)")
    private java.math.BigDecimal unitPrice = java.math.BigDecimal.ZERO;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "total_amount", nullable = false, columnDefinition = "decimal(19,4)")
    private java.math.BigDecimal totalAmount = java.math.BigDecimal.ZERO;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SupplierInvoice getInvoice() {
        return invoice;
    }

    public void setInvoice(SupplierInvoice invoice) {
        this.invoice = invoice;
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

    public java.math.BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(java.math.BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = java.math.BigDecimal.valueOf(unitPrice);
    }

    public java.math.BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(java.math.BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = java.math.BigDecimal.valueOf(totalAmount);
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

}