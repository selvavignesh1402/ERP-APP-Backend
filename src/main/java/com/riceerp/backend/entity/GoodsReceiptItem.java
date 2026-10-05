package com.riceerp.backend.entity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import org.hibernate.annotations.TenantId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "goods_receipt_items")
public class GoodsReceiptItem {

    @TenantId
    @Column(name = "organization_id")
    private Long organizationId;


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id", nullable = false)
    @JsonIgnore
    private GoodsReceipt receipt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "ordered_qty", nullable = false, columnDefinition = "decimal(19,6)")
    private double orderedQty;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "received_qty", nullable = false, columnDefinition = "decimal(19,6)")
    private double receivedQty;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "unit_price", nullable = false, columnDefinition = "decimal(19,4)")
    private double unitPrice;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public GoodsReceipt getReceipt() {
        return receipt;
    }

    public void setReceipt(GoodsReceipt receipt) {
        this.receipt = receipt;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public double getOrderedQty() {
        return orderedQty;
    }

    public void setOrderedQty(double orderedQty) {
        this.orderedQty = orderedQty;
    }

    public double getReceivedQty() {
        return receivedQty;
    }

    public void setReceivedQty(double receivedQty) {
        this.receivedQty = receivedQty;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

}