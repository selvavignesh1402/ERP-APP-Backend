package com.riceerp.backend.dto;

import java.util.List;

public class GoodsReceiptRequest {
    private String receiptNumber;
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