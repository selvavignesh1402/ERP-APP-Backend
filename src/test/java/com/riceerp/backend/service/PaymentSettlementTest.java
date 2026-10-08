package com.riceerp.backend.service;

import com.riceerp.backend.dto.PaymentRequest;
import com.riceerp.backend.entity.*;
import com.riceerp.backend.enums.*;
import com.riceerp.backend.exception.*;
import com.riceerp.backend.repository.*;
import com.riceerp.backend.security.TenantContext;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentSettlementTest {
    final PaymentRepository payments = mock(PaymentRepository.class);
    final SaleRepository sales = mock(SaleRepository.class);
    final CustomerRepository customers = mock(CustomerRepository.class);
    final OrganizationRepository organizations = mock(OrganizationRepository.class);
    final PurchaseRepository purchases = mock(PurchaseRepository.class);
    final PaymentService service = new PaymentService(payments, sales, customers, organizations, purchases);
    final Customer customer = new Customer();
    final Sale first = new Sale();
    final Sale second = new Sale();
    PaymentRequest request;

    @BeforeEach void setup() {
        when(payments.sumByReference(any(), any())).thenReturn(BigDecimal.ZERO);
        when(payments.sumAllocatedToSale(any())).thenReturn(BigDecimal.ZERO);
        TenantContext.setCurrentTenant(1L);
        ReflectionTestUtils.setField(customer, "id", 2L); customer.setCreditBalance(150);
        first.setId(3L); first.setCustomer(customer); first.setPaymentMode(PaymentMode.CREDIT); first.setGrandTotal(100);
        second.setId(4L); second.setCustomer(customer); second.setPaymentMode(PaymentMode.CREDIT); second.setGrandTotal(50);
        when(organizations.lockForPayment(1L)).thenReturn(Optional.of(new Organization()));
        when(sales.findByIdAndOrganizationId(3L, 1L)).thenReturn(Optional.of(first));
        when(customers.findByIdAndOrganizationId(2L, 1L)).thenReturn(Optional.of(customer));
        when(sales.findByCustomerIdAndOrganizationIdAndPaymentModeOrderBySaleDateAscIdAsc(2L, 1L, PaymentMode.CREDIT))
                .thenReturn(List.of(first, second));
        when(payments.save(any())).thenAnswer(i -> i.getArgument(0));
        request = new PaymentRequest(); request.setReferenceType("SALE"); request.setReferenceId(3L);
        request.setAmount(new BigDecimal("100")); request.setPaymentMode("CASH"); request.setClientReferenceId("request-1");
    }
    @AfterEach void cleanup() { TenantContext.clear(); }
    void noWrites() { verify(payments, never()).save(any()); verify(customers, never()).save(any()); }
    @Test void purchasePaymentCanSettleTheExactLastPaisa() {
        var purchase = new Purchase(); purchase.setId(8L); purchase.setStatus(PurchaseStatus.APPROVED);
        purchase.setTotalAmount(new BigDecimal("123456789012345.67"));
        when(purchases.findByIdAndOrganizationId(8L, 1L)).thenReturn(Optional.of(purchase));
        when(payments.sumByReference(ReferenceType.PURCHASE, 8L)).thenReturn(new BigDecimal("123456789012345.66"));
        request.setReferenceType("PURCHASE"); request.setReferenceId(8L); request.setAmount(new BigDecimal("0.02"));
        assertThrows(BusinessRuleException.class, () -> service.createPayment(request)); noWrites();
        request.setAmount(new BigDecimal("0.01"));
        assertEquals(new BigDecimal("0.01"), service.createPayment(request).getAmount());
    }

    @Test void settlingOneInvoiceDoesNotEraseOtherInvoiceDebt() {
        service.createPayment(request); assertEquals(0, new BigDecimal("50").compareTo(customer.getCreditBalance()));
        when(payments.sumByReference(ReferenceType.SALE, 3L)).thenReturn(BigDecimal.valueOf(100.0));
        request.setClientReferenceId("different-request"); request.setAmount(new BigDecimal("10"));
        assertThrows(BusinessRuleException.class, () -> service.createPayment(request));
        assertEquals(0, new BigDecimal("50").compareTo(customer.getCreditBalance())); verify(payments, times(1)).save(any());
    }
    @Test void existingPartialPaymentLimitsFurtherCollection() {
        when(payments.sumByReference(ReferenceType.SALE, 3L)).thenReturn(BigDecimal.valueOf(40.0)); customer.setCreditBalance(110);
        request.setAmount(new BigDecimal("61")); assertThrows(BusinessRuleException.class, () -> service.createPayment(request)); noWrites();
        request.setAmount(new BigDecimal("60")); service.createPayment(request); assertEquals(0, new BigDecimal("50").compareTo(customer.getCreditBalance()));
    }
    @Test void priorCustomerAllocationAlsoReducesInvoiceDue() {
        when(payments.sumAllocatedToSale(3L)).thenReturn(BigDecimal.valueOf(100.0)); customer.setCreditBalance(50);
        assertThrows(BusinessRuleException.class, () -> service.createPayment(request)); noWrites();
    }
    @Test void customerCollectionAllocatesOldestInvoicesOnce() {
        request.setReferenceType("CUSTOMER"); request.setReferenceId(2L); request.setAmount(new BigDecimal("120"));
        Payment saved = service.createPayment(request);
        assertEquals(Map.of(3L, new BigDecimal("100.00"), 4L, new BigDecimal("20.00")), saved.getSaleAllocations());
        assertEquals(0, saved.getOpeningBalanceAmount().signum()); assertEquals(0, new BigDecimal("30").compareTo(customer.getCreditBalance()));
    }
    @Test void customerCollectionCanSettleExplicitOpeningDebt() {
        customer.setCreditBalance(180); request.setReferenceType("CUSTOMER"); request.setReferenceId(2L); request.setAmount(new BigDecimal("170"));
        Payment saved = service.createPayment(request);
        assertEquals(0, new BigDecimal("20").compareTo(saved.getOpeningBalanceAmount())); assertEquals(0, new BigDecimal("10").compareTo(customer.getCreditBalance()));
        assertEquals(0, new BigDecimal("150").compareTo(saved.getSaleAllocations().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)));
    }
    @Test void fractionalCollectionAllocatesExactlyAndLeavesRemainingDebt() {
        first.setGrandTotal(100.10);
        second.setGrandTotal(50.20);
        customer.setCreditBalance(150.30);
        request.setReferenceType("CUSTOMER");
        request.setReferenceId(2L);
        request.setAmount(new BigDecimal("120.30"));
        Payment saved = service.createPayment(request);
        assertEquals(new BigDecimal("120.30"), saved.getAmount());
        assertEquals(Map.of(3L, new BigDecimal("100.10"), 4L, new BigDecimal("20.20")), saved.getSaleAllocations());
        assertEquals(0, new BigDecimal("30.00").compareTo(customer.getCreditBalance()));
        assertEquals(0, saved.getOpeningBalanceAmount().signum());
    }

    @Test void smallPaymentReducesLargeCustomerBalanceByExactlyOneCent() {
        customer.setCreditBalance(new BigDecimal("123456789012345.67"));
        request.setReferenceType("CUSTOMER");
        request.setReferenceId(2L);
        request.setAmount(new BigDecimal("0.01"));
        service.createPayment(request);
        assertEquals(new BigDecimal("123456789012345.66"), customer.getCreditBalance());
    }

    @Test void exactLargeRequestSettlesAndRetriesWithoutLosingACent() {
        customer.setCreditBalance(new BigDecimal("123456789012345.68"));
        request.setReferenceType("CUSTOMER");
        request.setReferenceId(2L);
        request.setAmount(new BigDecimal("123456789012345.67"));
        Payment saved = service.createPayment(request);
        assertEquals(new BigDecimal("123456789012345.67"), saved.getAmount());
        assertEquals(new BigDecimal("0.01"), customer.getCreditBalance());
        assertEquals(new BigDecimal("123456789012195.67"), saved.getOpeningBalanceAmount());
        when(payments.findByOrganizationIdAndClientReferenceId(1L, "request-1")).thenReturn(Optional.of(saved));
        request.setAmount(new BigDecimal("123456789012345.6700"));
        assertSame(saved, service.createPayment(request));
        verify(payments, times(1)).save(any());
        verify(customers, times(1)).save(any());
        request.setAmount(new BigDecimal("123456789012345.68"));
        assertThrows(BusinessRuleException.class, () -> service.createPayment(request));
    }

    @Test void overpaymentIsRejectedWithoutClampingBalance() {
        request.setReferenceType("CUSTOMER"); request.setReferenceId(2L); request.setAmount(new BigDecimal("151"));
        assertThrows(BusinessRuleException.class, () -> service.createPayment(request));
        assertEquals(0, new BigDecimal("150").compareTo(customer.getCreditBalance())); noWrites();
    }
    @Test void historicalUnallocatedCollectionsRequireReconciliationForBothPaths() {
        customer.setCreditBalance(120); request.setAmount(new BigDecimal("10"));
        assertThrows(BusinessRuleException.class, () -> service.createPayment(request));
        request.setReferenceType("CUSTOMER"); request.setReferenceId(2L);
        assertThrows(BusinessRuleException.class, () -> service.createPayment(request)); noWrites();
    }
    @Test void retryReturnsSamePaymentWithoutAnotherBalanceChange() {
        Payment saved = service.createPayment(request);
        when(payments.findByOrganizationIdAndClientReferenceId(1L, "request-1")).thenReturn(Optional.of(saved));
        assertSame(saved, service.createPayment(request)); assertEquals(0, new BigDecimal("50").compareTo(customer.getCreditBalance()));
        verify(payments, times(1)).save(any()); verify(customers, times(1)).save(any());
        var order = inOrder(organizations, payments); order.verify(organizations).lockForPayment(1L);
        order.verify(payments).findByOrganizationIdAndClientReferenceId(1L, "request-1");
    }
    @Test void retryKeyCannotBeUsedWithDifferentPaymentDetails() {
        Payment saved = service.createPayment(request);
        when(payments.findByOrganizationIdAndClientReferenceId(1L, "request-1")).thenReturn(Optional.of(saved));
        request.setAmount(new BigDecimal("20")); assertThrows(BusinessRuleException.class, () -> service.createPayment(request));
        verify(payments, times(1)).save(any());
    }
    @ParameterizedTest @org.junit.jupiter.params.provider.NullSource @ValueSource(strings = {"0", "-1", "1.001", "1000000000000000", "1E+30", "1E-30"})
    void invalidAmountsAreRejected(String amount) {
        request.setAmount(amount == null ? null : new BigDecimal(amount)); assertThrows(BusinessRuleException.class, () -> service.createPayment(request)); noWrites();
    }
    @Test void invalidModeOrMissingRequestIdRejected() {
        request.setPaymentMode("CREDIT"); assertThrows(BusinessRuleException.class, () -> service.createPayment(request));
        request.setPaymentMode("CASH"); request.setClientReferenceId(null);
        assertThrows(BusinessRuleException.class, () -> service.createPayment(request)); noWrites();
    }
    @Test void nonexistentAndOtherOrganizationReferencesAreRejected() {
        request.setReferenceId(999L); assertThrows(NotFoundException.class, () -> service.createPayment(request));
        request.setReferenceType("CUSTOMER"); assertThrows(NotFoundException.class, () -> service.createPayment(request));
        request.setReferenceType("PURCHASE"); assertThrows(NotFoundException.class, () -> service.createPayment(request)); noWrites();
    }
    @Test void cashInvoiceCannotReceiveAnotherPayment() {
        first.setPaymentMode(PaymentMode.CASH);
        assertThrows(BusinessRuleException.class, () -> service.createPayment(request)); noWrites();
    }
    @Test void purchasePaymentIsBoundedByItsOwnOutstandingAmount() {
        Purchase purchase = new Purchase(); purchase.setId(8L); purchase.setTotalAmount(200); purchase.setStatus(PurchaseStatus.APPROVED);
        when(purchases.findByIdAndOrganizationId(8L, 1L)).thenReturn(Optional.of(purchase));
        when(payments.sumByReference(ReferenceType.PURCHASE, 8L)).thenReturn(BigDecimal.valueOf(150.0));
        request.setReferenceType("PURCHASE"); request.setReferenceId(8L); request.setAmount(new BigDecimal("51"));
        assertThrows(BusinessRuleException.class, () -> service.createPayment(request)); noWrites();
        request.setAmount(new BigDecimal("50")); service.createPayment(request); assertEquals(0, new BigDecimal("150").compareTo(customer.getCreditBalance()));
    }
    @Test void cancelledPurchasesAndUnallocatedSupplierAdvancesAreRejected() {
        Purchase purchase = new Purchase(); purchase.setId(8L); purchase.setStatus(PurchaseStatus.CANCELLED);
        when(purchases.findByIdAndOrganizationId(8L, 1L)).thenReturn(Optional.of(purchase));
        request.setReferenceType("PURCHASE"); request.setReferenceId(8L);
        assertThrows(BusinessRuleException.class, () -> service.createPayment(request));
        request.setReferenceType("SUPPLIER"); assertThrows(BusinessRuleException.class, () -> service.createPayment(request)); noWrites();
    }
    @Test void invoicePaymentHistoryIncludesCustomerAllocations() {
        service.getPaymentsByReference("sale", 3L); verify(payments).findPaymentsForSale(3L);
        service.getPaymentsByReference("customer", 2L); verify(payments).findByReferenceTypeAndReferenceId(ReferenceType.CUSTOMER, 2L);
    }
}
