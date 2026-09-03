package com.riceerp.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class DeliveryCreateRequest {

    @NotNull(message = "Sales order ID is required")
    private Long salesOrderId;

    private Long deliveryPersonId;

    private String vehicleNumber;

    private String deliveryNotes;

    @NotEmpty(message = "Delivery note must contain at least one item")
    @Valid
    private List<DeliveryItemCreateRequest> items;

    public Long getSalesOrderId() {
        return salesOrderId;
    }

    public void setSalesOrderId(Long salesOrderId) {
        this.salesOrderId = salesOrderId;
    }

    public Long getDeliveryPersonId() {
        return deliveryPersonId;
    }

    public void setDeliveryPersonId(Long deliveryPersonId) {
        this.deliveryPersonId = deliveryPersonId;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public String getDeliveryNotes() {
        return deliveryNotes;
    }

    public void setDeliveryNotes(String deliveryNotes) {
        this.deliveryNotes = deliveryNotes;
    }

    public List<DeliveryItemCreateRequest> getItems() {
        return items;
    }

    public void setItems(List<DeliveryItemCreateRequest> items) {
        this.items = items;
    }
}
