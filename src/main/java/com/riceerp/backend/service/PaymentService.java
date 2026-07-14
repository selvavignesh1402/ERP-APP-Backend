package com.riceerp.backend.service;

import com.riceerp.backend.dto.PaymentRequest;
import com.riceerp.backend.entity.Payment;
import com.riceerp.backend.enums.PaymentMode;
import com.riceerp.backend.enums.ReferenceType;
import com.riceerp.backend.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public Payment createPayment(PaymentRequest request) {
        Payment payment = new Payment();
        payment.setReferenceType(ReferenceType.valueOf(request.getReferenceType().toUpperCase()));
        payment.setReferenceId(request.getReferenceId());
        payment.setAmount(request.getAmount());
        payment.setPaymentMode(PaymentMode.valueOf(request.getPaymentMode().toUpperCase()));
        payment.setPaymentDate(LocalDateTime.now());

        return paymentRepository.save(payment);
    }

    public List<Payment> getPaymentsByReference(String referenceType, Long referenceId) {
        return paymentRepository.findByReferenceTypeAndReferenceId(referenceType.toUpperCase(), referenceId);
    }

    public List<Payment> listAllPayments() {
        return paymentRepository.findAll();
    }
}
