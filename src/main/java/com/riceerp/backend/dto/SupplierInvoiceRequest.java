package com.riceerp.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class SupplierInvoiceRequest {
    private String invoiceNumber;
    private Long supplierId;
    private Long purchaseId;
    private LocalDateTime invoiceDate;
    private List<SupplierInvoiceItemRequest> items;

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public Long getPurchaseId() {
        return purchaseId;
    }

    public void setPurchaseId(Long purchaseId) {
        this.purchaseId = purchaseId;
    }

    public LocalDateTime getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(LocalDateTime invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public List<SupplierInvoiceItemRequest> getItems() {
        return items;
    }

    public void setItems(List<SupplierInvoiceItemRequest> items) {
        this.items = items;
    }
}