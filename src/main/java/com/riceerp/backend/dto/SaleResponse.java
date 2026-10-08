package com.riceerp.backend.dto;

import com.riceerp.backend.entity.Sale;
import com.riceerp.backend.enums.PaymentMode;

import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * Response shape for a sale.
 *
 * <p>Identity is stored twice on purpose: {@code customerName}/{@code customerPhone}/
 * {@code customerAddress}/{@code shopName} are the immutable invoice snapshot written
 * at sale time, while {@code customer} is the live customer record. Both are returned
 * because clients read each for a different reason, and the snapshot is the one that
 * must stay stable when master data is later edited.
 */
public class SaleResponse {

    private com.riceerp.backend.enums.TaxType taxType;
    public com.riceerp.backend.enums.TaxType getTaxType() { return taxType; }
    private Long id;
    private Long organizationId;
    private String billNumber;
    private CustomerResponse customer;
    private String customerName;
    private String customerPhone;
    private String customerAddress;
    private String shopName;
    private LocalDateTime saleDate;
    private PaymentMode paymentMode;
    private BigDecimal total;
    private BigDecimal discount;
    private BigDecimal cgst;
    private BigDecimal sgst;
    private BigDecimal igst;
    private BigDecimal grandTotal;
    private BigDecimal paidAmount;
    private BigDecimal balanceDue;
    private String clientReferenceId;
    private Long salesOrderId;
    private Long deliveryId;
    private LocalDateTime createdAt;

    public static SaleResponse from(Sale s) {
        if (s == null) return null;
        SaleResponse r = new SaleResponse();
        r.id = s.getId();
        r.taxType = s.getTaxType();
        r.organizationId = s.getOrganizationId();
        r.billNumber = s.getBillNumber();
        r.customer = CustomerResponse.from(s.getCustomer());
        r.customerName = s.getCustomerName();
        r.customerPhone = s.getCustomerPhone();
        r.customerAddress = s.getCustomerAddress();
        r.shopName = s.getShopName();
        r.saleDate = s.getSaleDate();
        r.paymentMode = s.getPaymentMode();
        r.total = s.getTotal();
        r.discount = s.getDiscount();
        r.cgst = s.getCgst();
        r.sgst = s.getSgst();
        r.igst = s.getIgst();
        r.grandTotal = s.getGrandTotal();
        r.paidAmount = s.getPaidAmount();
        r.balanceDue = s.getBalanceDue();
        r.clientReferenceId = s.getClientReferenceId();
        r.salesOrderId = s.getSalesOrderId();
        r.deliveryId = s.getDeliveryId();
        r.createdAt = s.getCreatedAt();
        return r;
    }

    public Long getId() { return id; }
    public Long getOrganizationId() { return organizationId; }
    public String getBillNumber() { return billNumber; }
    public CustomerResponse getCustomer() { return customer; }
    public String getCustomerName() { return customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public String getCustomerAddress() { return customerAddress; }
    public String getShopName() { return shopName; }
    public LocalDateTime getSaleDate() { return saleDate; }
    public PaymentMode getPaymentMode() { return paymentMode; }
    public BigDecimal getTotal() { return total; }
    public BigDecimal getDiscount() { return discount; }
    public BigDecimal getCgst() { return cgst; }
    public BigDecimal getSgst() { return sgst; }
    public BigDecimal getIgst() { return igst; }
    public BigDecimal getGrandTotal() { return grandTotal; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public BigDecimal getBalanceDue() { return balanceDue; }
    public String getClientReferenceId() { return clientReferenceId; }
    public Long getSalesOrderId() { return salesOrderId; }
    public Long getDeliveryId() { return deliveryId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}