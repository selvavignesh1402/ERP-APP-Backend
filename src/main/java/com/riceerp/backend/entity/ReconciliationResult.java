package com.riceerp.backend.entity;

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

    @Column(name = "amount_matched", nullable = false)
    private double amountMatched;

    @Column(name = "amount_on_purchase", nullable = false)
    private double amountOnPurchase;

    @Column(name = "amount_on_invoice", nullable = false)
    private double amountOnInvoice;

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

    public double getAmountMatched() {
        return amountMatched;
    }

    public void setAmountMatched(double amountMatched) {
        this.amountMatched = amountMatched;
    }

    public double getAmountOnPurchase() {
        return amountOnPurchase;
    }

    public void setAmountOnPurchase(double amountOnPurchase) {
        this.amountOnPurchase = amountOnPurchase;
    }

    public double getAmountOnInvoice() {
        return amountOnInvoice;
    }

    public void setAmountOnInvoice(double amountOnInvoice) {
        this.amountOnInvoice = amountOnInvoice;
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