package com.riceerp.backend.dto;

import com.riceerp.backend.enums.PurchaseStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class PurchaseRequest {
    @NotNull(message = "Supplier is required")
    private Long supplierId;

    @NotEmpty(message = "Purchase must contain at least one item")
    @Valid
    private List<PurchaseItemRequest> items;

    // Optional fields (invoiceNumber, status) — service forces its own defaults.
    private String invoiceNumber;
    private PurchaseStatus status;

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public PurchaseStatus getStatus() {
        return status;
    }

    public void setStatus(PurchaseStatus status) {
        this.status = status;
    }

    public List<PurchaseItemRequest> getItems() {
        return items;
    }

    public void setItems(List<PurchaseItemRequest> items) {
        this.items = items;
    }
}