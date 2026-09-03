package com.riceerp.backend.dto;

import com.riceerp.backend.enums.PaymentMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class DeliveryConfirmRequest {

    @NotBlank(message = "Receiver name is required for delivery proof")
    private String receiverName;

    private String receiverPhone;

    private String deliveryNotes;

    private PaymentMode paymentMode = PaymentMode.CREDIT;

    @NotEmpty(message = "Confirmation must specify delivered quantities")
    @Valid
    private List<DeliveryItemConfirmRequest> items;

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getReceiverPhone() {
        return receiverPhone;
    }

    public void setReceiverPhone(String receiverPhone) {
        this.receiverPhone = receiverPhone;
    }

    public String getDeliveryNotes() {
        return deliveryNotes;
    }

    public void setDeliveryNotes(String deliveryNotes) {
        this.deliveryNotes = deliveryNotes;
    }

    public PaymentMode getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(PaymentMode paymentMode) {
        this.paymentMode = paymentMode;
    }

    public List<DeliveryItemConfirmRequest> getItems() {
        return items;
    }

    public void setItems(List<DeliveryItemConfirmRequest> items) {
        this.items = items;
    }
}
