package com.riceerp.backend.entity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.riceerp.backend.enums.SalesOrderStatus;
import org.hibernate.annotations.TenantId;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

@Entity
@Table(name = "sales_orders")
public class SalesOrder {
    @Enumerated(EnumType.STRING)
    @Column(name = "tax_type", nullable = false, columnDefinition = "varchar(20) default 'INTRA_STATE'")
    private com.riceerp.backend.enums.TaxType taxType = com.riceerp.backend.enums.TaxType.INTRA_STATE;
    public com.riceerp.backend.enums.TaxType getTaxType() { return taxType; }
    public void setTaxType(com.riceerp.backend.enums.TaxType value) { taxType = value; }
    @Version
    @Column(nullable = false, columnDefinition = "bigint default 0")
    private Long version = 0L;

    @TenantId
    @Column(name = "organization_id")
    private Long organizationId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "salesperson_id")
    private User salesperson;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate = LocalDateTime.now();

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SalesOrderStatus status = SalesOrderStatus.CONFIRMED;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(nullable = false, columnDefinition = "decimal(19,4)")
    private BigDecimal subtotal = BigDecimal.ZERO;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(nullable = false, columnDefinition = "decimal(19,4)")
    private BigDecimal discount = BigDecimal.ZERO;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(nullable = false, columnDefinition = "decimal(19,4)")
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "grand_total", nullable = false, columnDefinition = "decimal(19,4)")
    private BigDecimal grandTotal = BigDecimal.ZERO;

    @Column(length = 500)
    private String notes;

    @OneToMany(mappedBy = "salesOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SalesOrderItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public User getSalesperson() {
        return salesperson;
    }

    public void setSalesperson(User salesperson) {
        this.salesperson = salesperson;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public LocalDate getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    public void setExpectedDeliveryDate(LocalDate expectedDeliveryDate) {
        this.expectedDeliveryDate = expectedDeliveryDate;
    }

    public SalesOrderStatus getStatus() {
        return status;
    }

    public void setStatus(SalesOrderStatus status) {
        this.status = status;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) { this.subtotal = BigDecimal.valueOf(subtotal); }
    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) { this.discount = BigDecimal.valueOf(discount); }
    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(double taxAmount) { this.taxAmount = BigDecimal.valueOf(taxAmount); }
    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getGrandTotal() {
        return grandTotal;
    }

    public void setGrandTotal(double grandTotal) { this.grandTotal = BigDecimal.valueOf(grandTotal); }
    public void setGrandTotal(BigDecimal grandTotal) {
        this.grandTotal = grandTotal;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<SalesOrderItem> getItems() {
        return items;
    }

    public void setItems(List<SalesOrderItem> items) {
        this.items = items;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }
}
