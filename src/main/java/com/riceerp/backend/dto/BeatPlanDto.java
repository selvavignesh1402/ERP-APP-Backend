package com.riceerp.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.util.List;

public class BeatPlanDto {

    private Long id;

    @NotBlank(message = "Beat plan name is required")
    private String name;

    @NotNull(message = "Salesperson is required")
    private Long salespersonId;

    private String salespersonName;
    private boolean isActive;

    @NotEmpty(message = "Beat plan must contain at least one entry")
    @Valid
    private List<EntryDto> entries;

    public static class EntryDto {
        private Long id;

        @NotNull(message = "Day of week is required")
        private DayOfWeek dayOfWeek;

        @NotNull(message = "Customer is required")
        private Long customerId;

        private String customerName;

        @NotNull(message = "Visit order is required")
        private int visitOrder;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public DayOfWeek getDayOfWeek() {
            return dayOfWeek;
        }

        public void setDayOfWeek(DayOfWeek dayOfWeek) {
            this.dayOfWeek = dayOfWeek;
        }

        public Long getCustomerId() {
            return customerId;
        }

        public void setCustomerId(Long customerId) {
            this.customerId = customerId;
        }

        public String getCustomerName() {
            return customerName;
        }

        public void setCustomerName(String customerName) {
            this.customerName = customerName;
        }

        public int getVisitOrder() {
            return visitOrder;
        }

        public void setVisitOrder(int visitOrder) {
            this.visitOrder = visitOrder;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public List<EntryDto> getEntries() {
        return entries;
    }

    public void setEntries(List<EntryDto> entries) {
        this.entries = entries;
    }
}
