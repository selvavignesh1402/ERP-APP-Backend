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
import com.riceerp.backend.repository.OrganizationRepository;
import com.riceerp.backend.repository.PurchaseRepository;
import com.riceerp.backend.entity.Purchase;
import com.riceerp.backend.enums.PurchaseStatus;
import com.riceerp.backend.exception.BusinessRuleException;
import com.riceerp.backend.security.TenantContext;
import org.springframework.security.access.AccessDeniedException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final SaleRepository saleRepository;
    private final CustomerRepository customerRepository;
    private final OrganizationRepository organizationRepository;
    private final PurchaseRepository purchaseRepository;

    public PaymentService(PaymentRepository paymentRepository,
                          SaleRepository saleRepository,
                          CustomerRepository customerRepository,
                          OrganizationRepository organizationRepository,
                          PurchaseRepository purchaseRepository) {
        this.paymentRepository = paymentRepository;
        this.saleRepository = saleRepository;
        this.customerRepository = customerRepository;
        this.organizationRepository = organizationRepository;
        this.purchaseRepository = purchaseRepository;
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public Payment createPayment(PaymentRequest request) {
        Long orgId = TenantContext.getCurrentTenant();
        if (orgId == null || orgId <= 0) throw new AccessDeniedException("Select an organization first");
        ReferenceType type = parseType(request.getReferenceType());
        PaymentMode mode;
        try { mode = PaymentMode.valueOf(request.getPaymentMode().toUpperCase(Locale.ROOT)); }
        catch (RuntimeException ex) { throw new BusinessRuleException("Select CASH, UPI or CARD for a payment"); }
        if (mode == PaymentMode.CREDIT) throw new BusinessRuleException("CREDIT is not a collected payment");
        if (request.getReferenceId() == null || request.getReferenceId() <= 0) {
            throw new BusinessRuleException("A valid payment reference is required");
        }
        BigDecimal requestedAmount = request.getAmount();
        if (requestedAmount == null) {
            throw new BusinessRuleException("Payment amount is required");
        }
        if (requestedAmount.signum() <= 0 || requestedAmount.stripTrailingZeros().scale() > 2) {
            throw new BusinessRuleException("Payment must be positive with at most two decimal places");
        }
        if (requestedAmount.compareTo(new BigDecimal("999999999999999.99")) > 0) {
            throw new BusinessRuleException("Payment amount exceeds the supported maximum");
        }
        BigDecimal amount = money(requestedAmount);
        String key = request.getClientReferenceId();
        if (key == null || key.isBlank() || key.length() > 64) {
            throw new BusinessRuleException("A payment request ID of at most 64 characters is required");
        }
        // Serialize settlement and idempotency decisions within this shop. Customer @Version
        // also detects a concurrent sale changing its aggregate balance.
        organizationRepository.lockForPayment(orgId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));
        var existing = paymentRepository.findByOrganizationIdAndClientReferenceId(orgId, key);
        if (existing.isPresent()) {
            Payment previous = existing.get();
            if (previous.getReferenceType() != type || !Objects.equals(previous.getReferenceId(), request.getReferenceId()) ||
                    previous.getPaymentMode() != mode || money(previous.getAmount()).compareTo(amount) != 0) {
                throw new BusinessRuleException("This payment request ID was already used with different details");
            }
            return previous;
        }
        Payment payment = new Payment();
        payment.setReferenceType(type);
        payment.setReferenceId(request.getReferenceId());
        payment.setAmount(amount);
        payment.setPaymentMode(mode);
        payment.setClientReferenceId(key);
        payment.setPaymentDate(LocalDateTime.now());
        switch (type) {
            case SALE -> settleSale(payment, orgId, amount);
            case CUSTOMER -> settleCustomer(payment, orgId, amount);
            case PURCHASE -> settlePurchase(payment, orgId, amount);
            case SUPPLIER -> throw new BusinessRuleException("Select a purchase invoice for supplier payments; advances are not supported");
        }
        return paymentRepository.save(payment);
    }

    private void settleCustomer(Payment payment, Long orgId, BigDecimal amount) {
        Customer customer = customerRepository.findByIdAndOrganizationId(payment.getReferenceId(), orgId)
                .orElseThrow(() -> new NotFoundException("Customer not found in selected organization"));
        BigDecimal balance = money(customer.getCreditBalance());
        requireWithin(amount, balance);
        List<Sale> invoices = saleRepository.findByCustomerIdAndOrganizationIdAndPaymentModeOrderBySaleDateAscIdAsc(
                customer.getId(), orgId, PaymentMode.CREDIT);
        var outstanding = new java.util.LinkedHashMap<Long, BigDecimal>();
        BigDecimal invoiceBalance = BigDecimal.ZERO;
        for (Sale sale : invoices) {
            BigDecimal due = saleDue(sale);
            outstanding.put(sale.getId(), due);
            invoiceBalance = invoiceBalance.add(due);
        }
        // Historical customer-level collections were unallocated. Do not guess which
        // invoices they settled or consume the same balance a second time.
        if (invoiceBalance.compareTo(balance) > 0) {
            throw new BusinessRuleException("Customer balance and unpaid invoices disagree; reconcile earlier payments before collecting more");
        }
        BigDecimal remaining = amount;
        for (var entry : outstanding.entrySet()) {
            BigDecimal allocated = remaining.min(entry.getValue());
            if (allocated.signum() > 0) payment.getSaleAllocations().put(entry.getKey(), allocated);
            remaining = remaining.subtract(allocated);
            if (remaining.signum() == 0) break;
        }
        // Any remainder settles explicitly recorded non-invoice/opening debt, never an advance.
        payment.setOpeningBalanceAmount(remaining);
        customer.setCreditBalance(balance.subtract(amount));
        customerRepository.save(customer);
    }

    private void settleSale(Payment payment, Long orgId, BigDecimal amount) {
        Sale sale = saleRepository.findByIdAndOrganizationId(payment.getReferenceId(), orgId)
                .orElseThrow(() -> new NotFoundException("Sale not found in selected organization"));
        if (sale.getPaymentMode() != PaymentMode.CREDIT || sale.getCustomer() == null) {
            throw new BusinessRuleException("This invoice does not carry customer credit");
        }
        requireWithin(amount, saleDue(sale));
        Customer customer = customerRepository.findByIdAndOrganizationId(sale.getCustomer().getId(), orgId)
                .orElseThrow(() -> new NotFoundException("Customer not found in selected organization"));
        BigDecimal balance = money(customer.getCreditBalance());
        requireWithin(amount, balance);
        BigDecimal allInvoicesDue = saleRepository.findByCustomerIdAndOrganizationIdAndPaymentModeOrderBySaleDateAscIdAsc(
                customer.getId(), orgId, PaymentMode.CREDIT).stream().map(this::saleDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (allInvoicesDue.compareTo(balance) > 0) {
            throw new BusinessRuleException("Customer balance and unpaid invoices disagree; reconcile earlier payments before collecting more");
        }
        customer.setCreditBalance(balance.subtract(amount));
        customerRepository.save(customer);
    }

    private void settlePurchase(Payment payment, Long orgId, BigDecimal amount) {
        Purchase purchase = purchaseRepository.findByIdAndOrganizationId(payment.getReferenceId(), orgId)
                .orElseThrow(() -> new NotFoundException("Purchase not found in selected organization"));
        if (purchase.getStatus() == null || purchase.getStatus() == PurchaseStatus.DRAFT ||
                purchase.getStatus() == PurchaseStatus.PENDING_APPROVAL || purchase.getStatus() == PurchaseStatus.CANCELLED) {
            throw new BusinessRuleException("Only an approved purchase can receive payments");
        }
        requireWithin(amount, money(purchase.getTotalAmount()).subtract(money(
                paymentRepository.sumByReference(ReferenceType.PURCHASE, purchase.getId()))));
    }

    private BigDecimal saleDue(Sale sale) {
        BigDecimal paid = money(paymentRepository.sumByReference(ReferenceType.SALE, sale.getId()))
                .add(money(paymentRepository.sumAllocatedToSale(sale.getId())));
        return money(sale.getGrandTotal()).subtract(paid).max(BigDecimal.ZERO);
    }
    private void requireWithin(BigDecimal amount, BigDecimal outstanding) {
        if (amount.compareTo(outstanding) > 0) {
            throw new BusinessRuleException("Payment exceeds outstanding amount of " + outstanding.max(BigDecimal.ZERO).toPlainString());
        }
    }
    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
    private BigDecimal money(double value) {
        if (!Double.isFinite(value)) throw new BusinessRuleException("Payment and balance amounts must be finite");
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
    private ReferenceType parseType(String type) {
        try { return ReferenceType.valueOf(type.toUpperCase(Locale.ROOT)); }
        catch (RuntimeException ex) { throw new BusinessRuleException("Invalid payment reference type"); }
    }

    public List<Payment> getPaymentsByReference(String referenceType, Long referenceId) {
        ReferenceType type = parseType(referenceType);
        return type == ReferenceType.SALE ? paymentRepository.findPaymentsForSale(referenceId)
                : paymentRepository.findByReferenceTypeAndReferenceId(type, referenceId);
    }

    public List<Payment> listAllPayments() {
        return paymentRepository.findAll();
    }
}
