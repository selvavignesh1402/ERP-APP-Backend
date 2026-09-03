package com.riceerp.backend.service;

import com.riceerp.backend.dto.PaymentRequest;
import com.riceerp.backend.entity.Customer;
import com.riceerp.backend.entity.Payment;
import com.riceerp.backend.entity.Sale;
import com.riceerp.backend.enums.PaymentMode;
import com.riceerp.backend.enums.ReferenceType;
import com.riceerp.backend.exception.NotFoundException;
import com.riceerp.backend.repository.CustomerRepository;
import com.riceerp.backend.repository.PaymentRepository;
import com.riceerp.backend.repository.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final SaleRepository saleRepository;
    private final CustomerRepository customerRepository;

    public PaymentService(PaymentRepository paymentRepository,
                          SaleRepository saleRepository,
                          CustomerRepository customerRepository) {
        this.paymentRepository = paymentRepository;
        this.saleRepository = saleRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public Payment createPayment(PaymentRequest request) {
        Payment payment = new Payment();
        payment.setReferenceType(ReferenceType.valueOf(request.getReferenceType().toUpperCase()));
        payment.setReferenceId(request.getReferenceId());
        payment.setAmount(request.getAmount());
        payment.setPaymentMode(PaymentMode.valueOf(request.getPaymentMode().toUpperCase()));
        payment.setPaymentDate(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);

        // A payment against a credit sale settles the customer's outstanding balance.
        if (payment.getReferenceType() == ReferenceType.SALE) {
            settleCustomerCredit(payment.getReferenceId(), payment.getAmount());
        } else if (payment.getReferenceType() == ReferenceType.CUSTOMER) {
            settleDirectCustomerPayment(payment.getReferenceId(), payment.getAmount());
        }

        return saved;
    }

    private void settleDirectCustomerPayment(Long customerId, double amountPaid) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found with id: " + customerId));

        double newBalance = customer.getCreditBalance() - amountPaid;
        customer.setCreditBalance(Math.max(0.0, newBalance));
        customerRepository.save(customer);
    }

    private void settleCustomerCredit(Long saleId, double amountPaid) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new NotFoundException("Sale not found with id: " + saleId));

        // Only credit sales carry an outstanding balance; cash/UPI/card sales were settled at creation.
        if (sale.getPaymentMode() != PaymentMode.CREDIT) {
            return;
        }

        Customer customer = sale.getCustomer();
        if (customer == null) {
            return; // walk-in sale — nothing to settle
        }

        double newBalance = customer.getCreditBalance() - amountPaid;
        customer.setCreditBalance(Math.max(0.0, newBalance));
        customerRepository.save(customer);
    }

    public List<Payment> getPaymentsByReference(String referenceType, Long referenceId) {
        return paymentRepository.findByReferenceTypeAndReferenceId(referenceType.toUpperCase(), referenceId);
    }

    public List<Payment> listAllPayments() {
        return paymentRepository.findAll();
    }
}
