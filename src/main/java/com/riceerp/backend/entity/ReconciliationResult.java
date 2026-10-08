package com.riceerp.backend.entity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import org.hibernate.annotations.TenantId;
import com.riceerp.backend.enums.ReconciliationStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reconciliation_results")
public class ReconciliationResult {

    @TenantId
    @Column(name = "organization_id")
    private Long organizationId;


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "purchase_id", nullable = false)
    private Purchase purchase;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "invoice_id", nullable = false)
    private SupplierInvoice invoice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReconciliationStatus status;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "amount_matched", nullable = false, columnDefinition = "decimal(19,4)")
    private java.math.BigDecimal amountMatched = java.math.BigDecimal.ZERO;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "amount_on_purchase", nullable = false, columnDefinition = "decimal(19,4)")
    private java.math.BigDecimal amountOnPurchase = java.math.BigDecimal.ZERO;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "amount_on_invoice", nullable = false, columnDefinition = "decimal(19,4)")
    private java.math.BigDecimal amountOnInvoice = java.math.BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "reconciled_at", nullable = false)
    private LocalDateTime reconciledAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Purchase getPurchase() {
        return purchase;
    }

    public void setPurchase(Purchase purchase) {
        this.purchase = purchase;
    }

    public SupplierInvoice getInvoice() {
        return invoice;
    }

    public void setInvoice(SupplierInvoice invoice) {
        this.invoice = invoice;
    }

    public ReconciliationStatus getStatus() {
        return status;
    }

    public void setStatus(ReconciliationStatus status) {
        this.status = status;
    }

    public java.math.BigDecimal getAmountMatched() {
        return amountMatched;
    }

    public void setAmountMatched(java.math.BigDecimal amountMatched) { this.amountMatched = amountMatched; }

    public void setAmountMatched(double amountMatched) {
        this.amountMatched = java.math.BigDecimal.valueOf(amountMatched);
    }

    public java.math.BigDecimal getAmountOnPurchase() {
        return amountOnPurchase;
    }

    public void setAmountOnPurchase(java.math.BigDecimal amountOnPurchase) { this.amountOnPurchase = amountOnPurchase; }

    public void setAmountOnPurchase(double amountOnPurchase) {
        this.amountOnPurchase = java.math.BigDecimal.valueOf(amountOnPurchase);
    }

    public java.math.BigDecimal getAmountOnInvoice() {
        return amountOnInvoice;
    }

    public void setAmountOnInvoice(java.math.BigDecimal amountOnInvoice) { this.amountOnInvoice = amountOnInvoice; }

    public void setAmountOnInvoice(double amountOnInvoice) {
        this.amountOnInvoice = java.math.BigDecimal.valueOf(amountOnInvoice);
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getReconciledAt() {
        return reconciledAt;
    }

    public void setReconciledAt(LocalDateTime reconciledAt) {
        this.reconciledAt = reconciledAt;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

}