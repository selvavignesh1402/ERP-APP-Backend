package com.riceerp.backend.entity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import org.hibernate.annotations.TenantId;
import com.riceerp.backend.enums.PaymentMode;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sales", uniqueConstraints = @UniqueConstraint(
        name = "uk_sale_org_client_reference", columnNames = {"organization_id", "client_reference_id"}))
public class Sale {
    // Response totals are calculated from persisted sale-linked payments on reads.
    @Transient
    private Double paidAmount;
    @Transient
    private Double balanceDue;
    public Double getPaidAmount() { return paidAmount; }
    public void setPaidAmount(Double amount) { paidAmount = amount; }
    public Double getBalanceDue() { return balanceDue; }
    public void setBalanceDue(Double amount) { balanceDue = amount; }

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
    private double total = 0.0;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(nullable = false, columnDefinition = "decimal(19,4)")
    private double discount = 0.0;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(nullable = false, columnDefinition = "decimal(19,4)")
    private double cgst = 0.0;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(nullable = false, columnDefinition = "decimal(19,4)")
    private double sgst = 0.0;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(nullable = false, columnDefinition = "decimal(19,4)")
    private double igst = 0.0;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "grand_total", nullable = false, columnDefinition = "decimal(19,4)")
    private double grandTotal = 0.0;

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

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getCgst() {
        return cgst;
    }

    public void setCgst(double cgst) {
        this.cgst = cgst;
    }

    public double getSgst() {
        return sgst;
    }

    public void setSgst(double sgst) {
        this.sgst = sgst;
    }

    public double getIgst() {
        return igst;
    }

    public void setIgst(double igst) {
        this.igst = igst;
    }

    public double getGrandTotal() {
        return grandTotal;
    }

    public void setGrandTotal(double grandTotal) {
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