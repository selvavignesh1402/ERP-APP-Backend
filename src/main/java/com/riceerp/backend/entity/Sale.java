package com.riceerp.backend.entity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import org.hibernate.annotations.TenantId;
import com.riceerp.backend.enums.PaymentMode;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "sales", uniqueConstraints = @UniqueConstraint(
        name = "uk_sale_org_client_reference", columnNames = {"organization_id", "client_reference_id"}))
public class Sale {
    @Enumerated(EnumType.STRING)
    @Column(name = "tax_type", nullable = false, columnDefinition = "varchar(20) default 'INTRA_STATE'")
    private com.riceerp.backend.enums.TaxType taxType = com.riceerp.backend.enums.TaxType.INTRA_STATE;
    public com.riceerp.backend.enums.TaxType getTaxType() { return taxType; }
    public void setTaxType(com.riceerp.backend.enums.TaxType value) { taxType = value; }
    // Response totals are calculated from persisted sale-linked payments on reads.
    @Transient
    private BigDecimal paidAmount;
    @Transient
    private BigDecimal balanceDue;
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(double amount) { paidAmount = BigDecimal.valueOf(amount); }
    public void setPaidAmount(BigDecimal amount) { paidAmount = amount; }
    public BigDecimal getBalanceDue() { return balanceDue; }
    public void setBalanceDue(double amount) { balanceDue = BigDecimal.valueOf(amount); }
    public void setBalanceDue(BigDecimal amount) { balanceDue = amount; }

    @TenantId
    @Column(name = "organization_id")
    private Long organizationId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bill_number", nullable = false, unique = true)
    private String billNumber;

    @Column(name = "customer_name")
    private String customerName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "sale_date", nullable = false)
    private LocalDateTime saleDate = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false)
    private PaymentMode paymentMode;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(nullable = false, columnDefinition = "decimal(19,4)")
    private BigDecimal total = BigDecimal.ZERO;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(nullable = false, columnDefinition = "decimal(19,4)")
    private BigDecimal discount = BigDecimal.ZERO;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(nullable = false, columnDefinition = "decimal(19,4)")
    private BigDecimal cgst = BigDecimal.ZERO;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(nullable = false, columnDefinition = "decimal(19,4)")
    private BigDecimal sgst = BigDecimal.ZERO;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(nullable = false, columnDefinition = "decimal(19,4)")
    private BigDecimal igst = BigDecimal.ZERO;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "grand_total", nullable = false, columnDefinition = "decimal(19,4)")
    private BigDecimal grandTotal = BigDecimal.ZERO;

    @Column(name = "client_reference_id", length = 64)
    private String clientReferenceId;

    @Column(name = "sales_order_id")
    private Long salesOrderId;

    @Column(name = "delivery_id")
    private Long deliveryId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "shop_name", updatable = false)
    private String shopName;
    @Column(name = "customer_phone", updatable = false)
    private String customerPhone;
    @Column(name = "customer_address", updatable = false)
    private String customerAddress;
    public String getShopName() { return shopName; }
    public void setShopName(String value) { shopName = value; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String value) { customerPhone = value; }
    public String getCustomerAddress() { return customerAddress; }
    public void setCustomerAddress(String value) { customerAddress = value; }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBillNumber() {
        return billNumber;
    }

    public void setBillNumber(String billNumber) {
        this.billNumber = billNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public LocalDateTime getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(LocalDateTime saleDate) {
        this.saleDate = saleDate;
    }

    public PaymentMode getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(PaymentMode paymentMode) {
        this.paymentMode = paymentMode;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(double total) { this.total = BigDecimal.valueOf(total); }
    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) { this.discount = BigDecimal.valueOf(discount); }
    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getCgst() {
        return cgst;
    }

    public void setCgst(double cgst) { this.cgst = BigDecimal.valueOf(cgst); }
    public void setCgst(BigDecimal cgst) {
        this.cgst = cgst;
    }

    public BigDecimal getSgst() {
        return sgst;
    }

    public void setSgst(double sgst) { this.sgst = BigDecimal.valueOf(sgst); }
    public void setSgst(BigDecimal sgst) {
        this.sgst = sgst;
    }

    public BigDecimal getIgst() {
        return igst;
    }

    public void setIgst(double igst) { this.igst = BigDecimal.valueOf(igst); }
    public void setIgst(BigDecimal igst) {
        this.igst = igst;
    }

    public BigDecimal getGrandTotal() {
        return grandTotal;
    }

    public void setGrandTotal(double grandTotal) { this.grandTotal = BigDecimal.valueOf(grandTotal); }
    public void setGrandTotal(BigDecimal grandTotal) {
        this.grandTotal = grandTotal;
    }

    public String getClientReferenceId() {
        return clientReferenceId;
    }

    public void setClientReferenceId(String clientReferenceId) {
        this.clientReferenceId = clientReferenceId;
    }

    public Long getSalesOrderId() {
        return salesOrderId;
    }

    public void setSalesOrderId(Long salesOrderId) {
        this.salesOrderId = salesOrderId;
    }

    public Long getDeliveryId() {
        return deliveryId;
    }

    public void setDeliveryId(Long deliveryId) {
        this.deliveryId = deliveryId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }
}