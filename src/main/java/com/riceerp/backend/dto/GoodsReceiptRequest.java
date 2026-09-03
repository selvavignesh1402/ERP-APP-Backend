package com.riceerp.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class GoodsReceiptRequest {
    private String receiptNumber;

    @NotEmpty(message = "Goods receipt must contain at least one item")
    @Valid
    private List<GoodsReceiptItemRequest> items;

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public List<GoodsReceiptItemRequest> getItems() {
        return items;
    }

    public void setItems(List<GoodsReceiptItemRequest> items) {
        this.items = items;
    }
}