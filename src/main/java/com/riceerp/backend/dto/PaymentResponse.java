package com.riceerp.backend.dto;

import com.riceerp.backend.entity.Payment;
import com.riceerp.backend.enums.PaymentMode;
import com.riceerp.backend.enums.ReferenceType;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.math.BigDecimal;

/**
 * Response shape for a payment. Keeps the previous entity serialization, including
 * the {@code saleAllocations} map, so allocation-aware clients keep working while
 * the {@link Payment} entity's {@code @ElementCollection} mapping stops leaking
 * into the API.
 */
public class PaymentResponse {

    private Long id;
    private Long organizationId;
    private String clientReferenceId;
    private ReferenceType referenceType;
    private Long referenceId;
    private BigDecimal amount;
    private PaymentMode paymentMode;
    private LocalDateTime paymentDate;
    private BigDecimal openingBalanceAmount;
    private Map<Long, BigDecimal> saleAllocations;

    public static PaymentResponse from(Payment p) {
        if (p == null) return null;
        PaymentResponse r = new PaymentResponse();
        r.id = p.getId();
        r.organizationId = p.getOrganizationId();
        r.clientReferenceId = p.getClientReferenceId();
        r.referenceType = p.getReferenceType();
        r.referenceId = p.getReferenceId();
        r.amount = p.getAmount();
        r.paymentMode = p.getPaymentMode();
        r.paymentDate = p.getPaymentDate();
        r.openingBalanceAmount = p.getOpeningBalanceAmount();
        r.saleAllocations = p.getSaleAllocations() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(p.getSaleAllocations());
        return r;
    }

    public Long getId() { return id; }
    public Long getOrganizationId() { return organizationId; }
    public String getClientReferenceId() { return clientReferenceId; }
    public ReferenceType getReferenceType() { return referenceType; }
    public Long getReferenceId() { return referenceId; }
    public BigDecimal getAmount() { return amount; }
    public PaymentMode getPaymentMode() { return paymentMode; }
    public LocalDateTime getPaymentDate() { return paymentDate; }
    public BigDecimal getOpeningBalanceAmount() { return openingBalanceAmount; }
    public Map<Long, BigDecimal> getSaleAllocations() { return saleAllocations; }
}