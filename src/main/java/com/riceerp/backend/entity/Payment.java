package com.riceerp.backend.entity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import org.hibernate.annotations.TenantId;
import com.riceerp.backend.enums.PaymentMode;
import com.riceerp.backend.enums.ReferenceType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "payments", uniqueConstraints = @UniqueConstraint(
        name = "uk_payment_org_request", columnNames = {"organization_id", "client_reference_id"}))
public class Payment {
    @Column(name = "client_reference_id", length = 64)
    private String clientReferenceId;

    // Customer collections retain their original reference and explicitly allocate to invoices.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "payment_sale_allocations", joinColumns = @JoinColumn(name = "payment_id"))
    @MapKeyColumn(name = "sale_id")
    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "amount", nullable = false, columnDefinition = "decimal(19,4)")
    private java.util.Map<Long, BigDecimal> saleAllocations = new java.util.LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "opening_balance_amount", columnDefinition = "decimal(19,4)")
    private BigDecimal openingBalanceAmount;

    public String getClientReferenceId() { return clientReferenceId; }
    public void setClientReferenceId(String id) { clientReferenceId = id; }
    public java.util.Map<Long, BigDecimal> getSaleAllocations() { return saleAllocations; }
    public BigDecimal getOpeningBalanceAmount() { return openingBalanceAmount; }
    public void setOpeningBalanceAmount(BigDecimal amount) { openingBalanceAmount = amount; }
    public void setOpeningBalanceAmount(double amount) { openingBalanceAmount = BigDecimal.valueOf(amount); }

    @TenantId
    @Column(name = "organization_id")
    private Long organizationId;


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false)
    private ReferenceType referenceType;

    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(nullable = false, columnDefinition = "decimal(19,4)")
    private BigDecimal amount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false)
    private PaymentMode paymentMode;

    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate = LocalDateTime.now();

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ReferenceType getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(ReferenceType referenceType) {
        this.referenceType = referenceType;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Long referenceId) {
        this.referenceId = referenceId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = BigDecimal.valueOf(amount);
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentMode getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(PaymentMode paymentMode) {
        this.paymentMode = paymentMode;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

}
