package com.riceerp.backend.dto;

import java.util.ArrayList;
import java.util.List;

public class SyncBatchResponse {

    private int totalProcessed;
    private int successCount;
    private int duplicateCount;
    private int failureCount;
    private List<SyncItemResult> results = new ArrayList<>();

    public static class SyncItemResult {
        private String clientReferenceId;
        private Long serverSaleId;
        private String billNumber;
        private String status; // "SYNCED", "ALREADY_SYNCED", "FAILED"
        private String errorMessage;

        public SyncItemResult() {}

        public SyncItemResult(String clientReferenceId, Long serverSaleId, String billNumber, String status, String errorMessage) {
            this.clientReferenceId = clientReferenceId;
            this.serverSaleId = serverSaleId;
            this.billNumber = billNumber;
            this.status = status;
            this.errorMessage = errorMessage;
        }

        public String getClientReferenceId() {
            return clientReferenceId;
        }

        public void setClientReferenceId(String clientReferenceId) {
            this.clientReferenceId = clientReferenceId;
        }

        public Long getServerSaleId() {
            return serverSaleId;
        }

        public void setServerSaleId(Long serverSaleId) {
            this.serverSaleId = serverSaleId;
        }

        public String getBillNumber() {
            return billNumber;
        }

        public void setBillNumber(String billNumber) {
            this.billNumber = billNumber;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    public int getTotalProcessed() {
        return totalProcessed;
    }

    public void setTotalProcessed(int totalProcessed) {
        this.totalProcessed = totalProcessed;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getDuplicateCount() {
        return duplicateCount;
    }

    public void setDuplicateCount(int duplicateCount) {
        this.duplicateCount = duplicateCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public List<SyncItemResult> getResults() {
        return results;
    }

    public void setResults(List<SyncItemResult> results) {
        this.results = results;
    }
}
