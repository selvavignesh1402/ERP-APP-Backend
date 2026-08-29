package com.riceerp.backend.dto;

import java.util.List;

public class ManagerDashboardDto {

    private List<SalespersonSummary> team;
    private List<AlertDto> alerts;

    public static class SalespersonSummary {
        private Long salespersonId;
        private String salespersonName;
        private long totalScheduled;
        private long completed;
        private long missed;
        private long pending;
        private double totalOrders;
        private double totalCollections;

        public Long getSalespersonId() {
            return salespersonId;
        }

        public void setSalespersonId(Long salespersonId) {
            this.salespersonId = salespersonId;
        }

        public String getSalespersonName() {
            return salespersonName;
        }

        public void setSalespersonName(String salespersonName) {
            this.salespersonName = salespersonName;
        }

        public long getTotalScheduled() {
            return totalScheduled;
        }

        public void setTotalScheduled(long totalScheduled) {
            this.totalScheduled = totalScheduled;
        }

        public long getCompleted() {
            return completed;
        }

        public void setCompleted(long completed) {
            this.completed = completed;
        }

        public long getMissed() {
            return missed;
        }

        public void setMissed(long missed) {
            this.missed = missed;
        }

        public long getPending() {
            return pending;
        }

        public void setPending(long pending) {
            this.pending = pending;
        }

        public double getTotalOrders() {
            return totalOrders;
        }

        public void setTotalOrders(double totalOrders) {
            this.totalOrders = totalOrders;
        }

        public double getTotalCollections() {
            return totalCollections;
        }

        public void setTotalCollections(double totalCollections) {
            this.totalCollections = totalCollections;
        }
    }

    public static class AlertDto {
        private String type; // "WARNING", "DANGER", "INFO"
        private String message;
        private Long customerId;
        private Long salespersonId;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Long getCustomerId() {
            return customerId;
        }

        public void setCustomerId(Long customerId) {
            this.customerId = customerId;
        }

        public Long getSalespersonId() {
            return salespersonId;
        }

        public void setSalespersonId(Long salespersonId) {
            this.salespersonId = salespersonId;
        }
    }

    public List<SalespersonSummary> getTeam() {
        return team;
    }

    public void setTeam(List<SalespersonSummary> team) {
        this.team = team;
    }

    public List<AlertDto> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<AlertDto> alerts) {
        this.alerts = alerts;
    }
}
