package com.riceerp.backend.controller;

import com.riceerp.backend.dto.PaymentRequest;
import com.riceerp.backend.entity.Payment;
import com.riceerp.backend.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('payment:create')")
    public Payment createPayment(@Valid @RequestBody PaymentRequest request) {
        return paymentService.createPayment(request);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('payment:view')")
    public List<Payment> listAllPayments() {
        return paymentService.listAllPayments();
    }

    @GetMapping("/reference")
    @PreAuthorize("hasAuthority('payment:view')")
    public List<Payment> getPaymentsByReference(@RequestParam String type, @RequestParam Long id) {
        return paymentService.getPaymentsByReference(type, id);
    }
}
