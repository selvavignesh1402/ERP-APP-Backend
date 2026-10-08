package com.riceerp.backend.controller;

import com.riceerp.backend.dto.PaymentRequest;
import com.riceerp.backend.dto.PaymentResponse;
import com.riceerp.backend.entity.Payment;
import com.riceerp.backend.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/payments", "/payments"})
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('payment:create')")
    public PaymentResponse createPayment(@Valid @RequestBody PaymentRequest request) {
        return PaymentResponse.from(paymentService.createPayment(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('payment:view')")
    public List<PaymentResponse> listAllPayments() {
        return paymentService.listAllPayments().stream().map(PaymentResponse::from).toList();
    }

    @GetMapping("/reference")
    @PreAuthorize("hasAuthority('payment:view')")
    public List<PaymentResponse> getPaymentsByReference(@RequestParam String type, @RequestParam Long id) {
        return paymentService.getPaymentsByReference(type, id).stream().map(PaymentResponse::from).toList();
    }
}
