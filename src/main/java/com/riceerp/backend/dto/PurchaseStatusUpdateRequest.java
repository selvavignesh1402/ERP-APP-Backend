package com.riceerp.backend.dto;

import com.riceerp.backend.enums.PurchaseStatus;

public class PurchaseStatusUpdateRequest {
    private PurchaseStatus status;

    public PurchaseStatus getStatus() {
        return status;
    }

    public void setStatus(PurchaseStatus status) {
        this.status = status;
    }
}