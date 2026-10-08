package com.riceerp.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riceerp.backend.entity.*;
import com.riceerp.backend.enums.*;
import com.riceerp.backend.exception.BusinessRuleException;
import com.riceerp.backend.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReconciliationTest {
    @Test void largeResultAndDetailKeepExactPaisaAndPrices() throws Exception {
        var exact = new java.math.BigDecimal("123456789012345.6789");
        var ordered = po(1, 1); ordered.setPrice(exact);
        var billed = bill(1, 1); billed.setUnitPrice(exact);
        when(items.findByPurchaseId(3L)).thenReturn(List.of(ordered));
        when(invoices.getInvoiceItems(5L)).thenReturn(List.of(billed));
        invoice.setTotalAmount(exact);
        var result = service.reconcile(3L, 5L);
        assertMoney("123456789012345.68", result.getAmountMatched());
        assertMoney("123456789012345.68", result.getAmountOnPurchase());
        assertMoney("123456789012345.68", result.getAmountOnInvoice());
        var json = new ObjectMapper().enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        var detail = json.readTree(result.getDetails()).get(0);
        for (String field : List.of("orderedAmount", "billedAmount")) {
            assertTrue(detail.get(field).isNumber());
            assertMoney("123456789012345.68", detail.get(field).decimalValue());
        }
        for (String field : List.of("orderedPrice", "billedPrice")) {
            assertTrue(detail.get(field).isNumber());
            assertEquals(0, exact.compareTo(detail.get(field).decimalValue()));
        }
    }
    @Test void roundingOrAggregateOverflowCannotSaveOrChangeInvoice() {
        for (String value : List.of("999999999999999.9999", "1000000000000000")) {
            var price = new java.math.BigDecimal(value);
            var ordered = po(1, 1); ordered.setPrice(price);
            var billed = bill(1, 1); billed.setUnitPrice(price);
            when(items.findByPurchaseId(3L)).thenReturn(List.of(ordered));
            when(invoices.getInvoiceItems(5L)).thenReturn(List.of(billed));
            invoice.setTotalAmount(price); invoice.setPurchase(null); invoice.setStatus(InvoiceStatus.RECEIVED);
            assertThrows(BusinessRuleException.class, () -> service.reconcile(3L, 5L));
            assertNull(invoice.getPurchase()); assertEquals(InvoiceStatus.RECEIVED, invoice.getStatus());
        }
        verify(invoices, never()).save(any()); verify(results, never()).save(any());
    }
    @Test void weightedPriceRetainsSixDecimalRounding() throws Exception {
        when(items.findByPurchaseId(3L)).thenReturn(List.of(po(1, 1), po(2, 2)));
        when(invoices.getInvoiceItems(5L)).thenReturn(List.of(bill(1, 1), bill(2, 2)));
        invoice.setTotalAmount(5);
        var result = service.reconcile(3L, 5L);
        var json = new ObjectMapper().enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        var detail = json.readTree(result.getDetails()).get(0);
        assertMoney("1.666667", detail.get("orderedPrice").decimalValue());
        assertMoney("1.666667", detail.get("billedPrice").decimalValue());
    }
    private static void assertMoney(String expected, java.math.BigDecimal actual) { assertEquals(0, new java.math.BigDecimal(expected).compareTo(actual)); }
    @Test void roundedInvoiceLinesMatchTheirStoredHeader() {
        when(items.findByPurchaseId(3L)).thenReturn(List.of(po(1, 1)));
        when(invoices.getInvoiceItems(5L)).thenReturn(List.of(bill(.00166, 1), bill(.00166, 1), bill(.00166, 1)));
        invoice.setTotalAmount(new java.math.BigDecimal("0.0051"));
        assertEquals(ReconciliationStatus.MATCHED, service.reconcile(3L, 5L).getStatus());
    }
    @Test void exactPricesMatchButOneTenThousandthDifferenceDoesNot() {
        var price = new java.math.BigDecimal("123456789012345.6789");
        var ordered = po(1, 1); ordered.setPrice(price);
        var billed = bill(1, 1); billed.setUnitPrice(price);
        when(items.findByPurchaseId(3L)).thenReturn(List.of(ordered));
        when(invoices.getInvoiceItems(5L)).thenReturn(List.of(billed));
        invoice.setTotalAmount(price);
        assertEquals(ReconciliationStatus.MATCHED, service.reconcile(3L, 5L).getStatus());
        billed.setUnitPrice(price.subtract(new java.math.BigDecimal("0.0001")));
        invoice.setTotalAmount(billed.getUnitPrice());
        assertEquals(ReconciliationStatus.MISMATCHED, service.reconcile(3L, 5L).getStatus());
    }
    final ReconciliationResultRepository results = mock(ReconciliationResultRepository.class);
    final PurchaseRepository purchases = mock(PurchaseRepository.class);
    final PurchaseItemRepository items = mock(PurchaseItemRepository.class);
    final SupplierInvoiceService invoices = mock(SupplierInvoiceService.class);
    final GoodsReceiptService receipts = mock(GoodsReceiptService.class);
    final ProcurementLock lock = mock(ProcurementLock.class);
    final ObjectMapper mapper = new ObjectMapper();
    final ReconciliationService service = new ReconciliationService(results, purchases, items, invoices, receipts, mapper, lock);
    final Purchase purchase = new Purchase();
    final Supplier supplier = mock(Supplier.class);
    final Product product = new Product();
    final SupplierInvoice invoice = new SupplierInvoice();
    @BeforeEach void setup() {
        when(lock.acquire()).thenReturn(1L); when(supplier.getId()).thenReturn(2L);
        purchase.setId(3L); purchase.setSupplier(supplier); product.setId(4L); product.setProductName("Rice");
        invoice.setId(5L); invoice.setSupplier(supplier); invoice.setPurchase(purchase); invoice.setTotalAmount(1000);
        when(purchases.findByIdAndOrganizationId(3L, 1L)).thenReturn(Optional.of(purchase));
        when(invoices.getInvoiceById(5L)).thenReturn(invoice);
        when(items.findByPurchaseId(3L)).thenReturn(List.of(po(10, 100)));
        when(invoices.getInvoiceItems(5L)).thenReturn(List.of(bill(10, 100)));
        when(invoices.getInvoicesForPurchase(3L)).thenReturn(List.of(invoice));
        when(receipts.getReceivedQuantities(3L)).thenReturn(Map.of(4L, 10.0));
        when(results.save(any())).thenAnswer(call -> call.getArgument(0));
    }
    PurchaseItem po(double quantity, double price) {
        PurchaseItem item = new PurchaseItem(); item.setProduct(product); item.setQuantity(quantity); item.setPrice(price); return item;
    }
    SupplierInvoiceItem bill(double quantity, double price) {
        SupplierInvoiceItem item = new SupplierInvoiceItem(); item.setProduct(product); item.setQuantity(quantity); item.setUnitPrice(price); return item;
    }
    SupplierInvoice other(double quantity, double price) {
        SupplierInvoice other = new SupplierInvoice(); other.setId(6L); other.setPurchase(purchase); other.setSupplier(supplier);
        other.setTotalAmount(quantity * price); other.setStatus(InvoiceStatus.PAID);
        when(invoices.getInvoiceItems(6L)).thenReturn(List.of(bill(quantity, price)));
        when(invoices.getInvoicesForPurchase(3L)).thenReturn(List.of(invoice, other)); return other;
    }
    @ParameterizedTest @ValueSource(doubles = {0, 6})
    void unreceivedOrInsufficientlyReceivedInvoiceCannotMatch(double received) {
        when(receipts.getReceivedQuantities(3L)).thenReturn(Map.of(4L, received));
        var result = service.reconcile(3L, 5L);
        assertEquals(ReconciliationStatus.MISMATCHED, result.getStatus()); assertMoney("0", result.getAmountMatched());
        assertEquals(InvoiceStatus.MISMATCHED, invoice.getStatus());
    }
    @Test void receivedOrderAndPriceMatch() {
        var result = service.reconcile(3L, 5L);
        assertEquals(ReconciliationStatus.MATCHED, result.getStatus()); assertMoney("1000", result.getAmountMatched());
    }
    @Test void legitimatePartialInvoiceMatchesReceivedPortion() {
        invoice.setTotalAmount(500); when(invoices.getInvoiceItems(5L)).thenReturn(List.of(bill(5, 100)));
        when(receipts.getReceivedQuantities(3L)).thenReturn(Map.of(4L, 5.0));
        assertEquals(ReconciliationStatus.MATCHED, service.reconcile(3L, 5L).getStatus());
    }
    @Test void cumulativeInvoicesCannotReuseReceivedOrOrderedQuantity() throws Exception {
        invoice.setTotalAmount(600); when(invoices.getInvoiceItems(5L)).thenReturn(List.of(bill(6, 100))); other(6, 100);
        var result = service.reconcile(3L, 5L);
        assertEquals(ReconciliationStatus.MISMATCHED, result.getStatus());
        var detail = mapper.readTree(result.getDetails()).get(0);
        assertEquals(6, detail.get("previouslyBilledQty").asInt()); assertEquals(4, detail.get("availableReceivedQty").asInt());
    }
    @Test void priorPaidInvoiceAndCurrentInvoiceCanExactlyFit() {
        invoice.setTotalAmount(400); when(invoices.getInvoiceItems(5L)).thenReturn(List.of(bill(4, 100))); other(6, 100);
        assertEquals(ReconciliationStatus.MATCHED, service.reconcile(3L, 5L).getStatus());
        assertMoney("400", service.reconcile(3L, 5L).getAmountMatched()); // repeating a reconciliation consumes nothing again
    }
    @Test void duplicateLinesCannotHideDifferentPriceBehindLastLine() {
        invoice.setTotalAmount(1500); when(invoices.getInvoiceItems(5L)).thenReturn(List.of(bill(5, 200), bill(5, 100)));
        assertEquals(ReconciliationStatus.MISMATCHED, service.reconcile(3L, 5L).getStatus());
    }
    @Test void matchingAverageCannotHideUnagreedPrices() {
        when(invoices.getInvoiceItems(5L)).thenReturn(List.of(bill(5, 50), bill(5, 150)));
        assertEquals(ReconciliationStatus.MISMATCHED, service.reconcile(3L, 5L).getStatus());
    }
    @Test void legitimateDuplicateLinesAndMultipleAgreedPricesMatchOnce() {
        when(items.findByPurchaseId(3L)).thenReturn(List.of(po(5, 100), po(5, 200)));
        when(invoices.getInvoiceItems(5L)).thenReturn(List.of(bill(2, 100), bill(3, 100), bill(5, 200))); invoice.setTotalAmount(1500);
        var result = service.reconcile(3L, 5L);
        assertEquals(ReconciliationStatus.MATCHED, result.getStatus()); assertMoney("1500", result.getAmountMatched());
        assertMoney("1500", result.getAmountOnPurchase());
    }
    @Test void unknownProductMismatchHasNoMatchedAmount() {
        Product unknown = new Product(); unknown.setId(9L); var item = bill(10, 100); item.setProduct(unknown);
        when(invoices.getInvoiceItems(5L)).thenReturn(List.of(item));
        var result = service.reconcile(3L, 5L); assertEquals(ReconciliationStatus.MISMATCHED, result.getStatus()); assertMoney("0", result.getAmountMatched());
    }
    @Test void corruptedInvoiceHeaderCannotMatchItsLines() {
        invoice.setTotalAmount(900); var result = service.reconcile(3L, 5L);
        assertEquals(ReconciliationStatus.MISMATCHED, result.getStatus()); assertMoney("0", result.getAmountMatched());
    }
    @ParameterizedTest @ValueSource(booleans = {true, false})
    void supplierMismatchRejectedEvenWhenAlreadyLinked(boolean linked) {
        Supplier wrong = mock(Supplier.class); when(wrong.getId()).thenReturn(99L); invoice.setSupplier(wrong);
        if (!linked) invoice.setPurchase(null);
        assertThrows(BusinessRuleException.class, () -> service.reconcile(3L, 5L));
        verify(results, never()).save(any()); verify(invoices, never()).save(any());
    }
    @Test void invoiceLinkedToAnotherPurchaseRejected() {
        Purchase wrong = new Purchase(); wrong.setId(99L); invoice.setPurchase(wrong);
        assertThrows(BusinessRuleException.class, () -> service.reconcile(3L, 5L)); verify(results, never()).save(any());
    }
    @Test void paidStatusIsPreservedWhileReconciliationReportsMismatch() {
        invoice.setStatus(InvoiceStatus.PAID); when(receipts.getReceivedQuantities(3L)).thenReturn(Map.of());
        assertEquals(ReconciliationStatus.MISMATCHED, service.reconcile(3L, 5L).getStatus()); assertEquals(InvoiceStatus.PAID, invoice.getStatus());
    }
    @Test void matchingUnlinkedInvoiceIsLinkedAfterValidation() {
        invoice.setPurchase(null); service.reconcile(3L, 5L); assertSame(purchase, invoice.getPurchase()); verify(invoices).save(invoice);
    }
    @ParameterizedTest @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, -1, 0})
    void invalidInvoiceQuantityCannotMatch(double quantity) {
        when(invoices.getInvoiceItems(5L)).thenReturn(List.of(bill(quantity, 100)));
        assertThrows(BusinessRuleException.class, () -> service.reconcile(3L, 5L)); verify(results, never()).save(any());
    }
}
