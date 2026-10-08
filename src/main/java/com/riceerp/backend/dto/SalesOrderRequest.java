package com.riceerp.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;

public class SalesOrderRequest {
    private com.riceerp.backend.enums.TaxType taxType = com.riceerp.backend.enums.TaxType.INTRA_STATE;
    public com.riceerp.backend.enums.TaxType getTaxType() { return taxType; }
    public void setTaxType(com.riceerp.backend.enums.TaxType value) { taxType = value; }

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    private Long salespersonId;

    private LocalDate expectedDeliveryDate;

    @NotNull(message = "Discount is required")
    @jakarta.validation.constraints.PositiveOrZero(message = "Discount must be zero or greater")
    @jakarta.validation.constraints.Digits(integer = 15, fraction = 2, message = "Discount must have at most 15 integer digits and two decimal places")
    private BigDecimal discount = BigDecimal.ZERO;

    private String notes;

    @NotEmpty(message = "Sales order must contain at least one item")
    @Valid
    private List<SalesOrderItemRequest> items;

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

    public LocalDate getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    public void setExpectedDeliveryDate(LocalDate expectedDeliveryDate) {
        this.expectedDeliveryDate = expectedDeliveryDate;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount == null ? null : discount.stripTrailingZeros();
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<SalesOrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<SalesOrderItemRequest> items) {
        this.items = items;
    }
}
