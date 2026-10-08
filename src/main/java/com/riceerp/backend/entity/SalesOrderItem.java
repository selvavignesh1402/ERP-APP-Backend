package com.riceerp.backend.entity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "sales_order_items")
public class SalesOrderItem {
    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "gst_rate", columnDefinition = "decimal(7,4) default 5", updatable = false)
    private Double gstRate;
    public Double getGstRate() { return gstRate; }
    public void setGstRate(Double value) { gstRate = value; }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_id", nullable = false)
    @JsonIgnore
    private SalesOrder salesOrder;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "ordered_quantity", nullable = false)
    private int orderedQuantity;

    @Column(name = "packed_quantity", nullable = false)
    private int packedQuantity = 0;

    @Column(name = "delivered_quantity", nullable = false)
    private int deliveredQuantity = 0;

    @Column(name = "remaining_quantity", nullable = false)
    private int remainingQuantity;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "unit_price", nullable = false, columnDefinition = "decimal(19,4)")
    private java.math.BigDecimal unitPrice = java.math.BigDecimal.ZERO;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "total_price", nullable = false, columnDefinition = "decimal(19,4)")
    private java.math.BigDecimal totalPrice = java.math.BigDecimal.ZERO;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SalesOrder getSalesOrder() {
        return salesOrder;
    }

    public void setSalesOrder(SalesOrder salesOrder) {
        this.salesOrder = salesOrder;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public int getOrderedQuantity() {
        return orderedQuantity;
    }

    public void setOrderedQuantity(int orderedQuantity) {
        this.orderedQuantity = orderedQuantity;
    }

    public int getPackedQuantity() {
        return packedQuantity;
    }

    public void setPackedQuantity(int packedQuantity) {
        this.packedQuantity = packedQuantity;
    }

    public int getDeliveredQuantity() {
        return deliveredQuantity;
    }

    public void setDeliveredQuantity(int deliveredQuantity) {
        this.deliveredQuantity = deliveredQuantity;
    }

    public int getRemainingQuantity() {
        return remainingQuantity;
    }

    public void setRemainingQuantity(int remainingQuantity) {
        this.remainingQuantity = remainingQuantity;
    }

    public java.math.BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) { this.unitPrice = java.math.BigDecimal.valueOf(unitPrice); }
    public void setUnitPrice(java.math.BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public java.math.BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) { this.totalPrice = java.math.BigDecimal.valueOf(totalPrice); }
    public void setTotalPrice(java.math.BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }
}
