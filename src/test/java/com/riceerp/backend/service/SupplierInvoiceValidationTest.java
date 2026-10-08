package com.riceerp.backend.service;

import com.riceerp.backend.dto.*;
import com.riceerp.backend.entity.*;
import com.riceerp.backend.enums.InvoiceStatus;
import com.riceerp.backend.exception.BusinessRuleException;
import com.riceerp.backend.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SupplierInvoiceValidationTest {
    @Test void exactPriceAndTotalsSurviveCreation() {
        var request = request(); var price = new java.math.BigDecimal("123456789012345.6789");
        request.getItems().get(0).setUnitPrice(price);
        var invoice = service.createInvoice(request);
        var expected = price.multiply(java.math.BigDecimal.valueOf(2));
        assertEquals(expected, invoice.getTotalAmount());
        verify(invoices, times(1)).save(any());
        verify(items).save(argThat(item -> price.equals(item.getUnitPrice()) && expected.equals(item.getTotalAmount()) && item.getInvoice() == invoice));
    }
    @Test void totalEqualsSumOfStoredFourDecimalLines() {
        var request = request(); var line = request.getItems().get(0);
        line.setQuantity(.123456); line.setUnitPrice(1);
        request.setItems(List.of(line, line));
        assertEquals(new java.math.BigDecimal("0.2470"), service.createInvoice(request).getTotalAmount());
        verify(items, times(2)).save(argThat(item -> new java.math.BigDecimal("0.1235").equals(item.getTotalAmount())));
    }
    @Test void invalidLaterPriceDoesNotSaveAnyInvoiceOrLine() {
        for (String price : new String[]{null, "0", "-1", "1000000000000000", "1.00001"}) {
            var request = request(); var bad = new SupplierInvoiceItemRequest();
            bad.setProductId(4L); bad.setQuantity(1); bad.setUnitPrice(price == null ? null : new java.math.BigDecimal(price));
            request.setItems(List.of(request.getItems().get(0), bad));
            assertThrows(BusinessRuleException.class, () -> service.createInvoice(request));
        }
        verify(invoices, never()).save(any()); verify(items, never()).save(any());
    }
    @Test void overflowingLineOrCombinedTotalDoesNotSave() {
        for (double quantity : new double[]{1, 2}) {
            var request = request(); var line = request.getItems().get(0);
            line.setQuantity(quantity); line.setUnitPrice(new java.math.BigDecimal("999999999999999.9999"));
            request.setItems(List.of(line, line));
            assertThrows(BusinessRuleException.class, () -> service.createInvoice(request));
        }
        verify(invoices, never()).save(any()); verify(items, never()).save(any());
    }
    @Test void invalidQuantityAndMissingProductCannotSave() {
        for (double quantity : new double[]{0, -1, 1e13, .0000001}) {
            var request = request(); request.getItems().get(0).setQuantity(quantity);
            assertThrows(BusinessRuleException.class, () -> service.createInvoice(request));
        }
        var request = request(); request.getItems().get(0).setProductId(null);
        assertThrows(BusinessRuleException.class, () -> service.createInvoice(request));
        request.setItems(Arrays.asList(request.getItems().get(0), null));
        assertThrows(BusinessRuleException.class, () -> service.createInvoice(request));
        verify(invoices, never()).save(any()); verify(items, never()).save(any());
    }
    final SupplierInvoiceRepository invoices = mock(SupplierInvoiceRepository.class);
    final SupplierInvoiceItemRepository items = mock(SupplierInvoiceItemRepository.class);
    final SupplierRepository suppliers = mock(SupplierRepository.class);
    final PurchaseRepository purchases = mock(PurchaseRepository.class);
    final ProductRepository products = mock(ProductRepository.class);
    final ProcurementLock lock = mock(ProcurementLock.class);
    final SupplierInvoiceService service = new SupplierInvoiceService(invoices, items, suppliers, purchases, products, lock);
    final Purchase purchase = new Purchase();
    final Supplier supplier = mock(Supplier.class);
    @BeforeEach void setup() {
        when(lock.acquire()).thenReturn(1L); when(lock.tenant()).thenReturn(1L); when(supplier.getId()).thenReturn(2L);
        purchase.setId(3L); purchase.setSupplier(supplier);
        when(purchases.findByIdAndOrganizationId(3L, 1L)).thenReturn(Optional.of(purchase));
        when(suppliers.findById(2L)).thenReturn(Optional.of(supplier));
        when(products.findById(4L)).thenReturn(Optional.of(new Product()));
        when(invoices.save(any())).thenAnswer(call -> call.getArgument(0));
    }
    SupplierInvoiceRequest request() {
        var line = new SupplierInvoiceItemRequest(); line.setProductId(4L); line.setQuantity(2); line.setUnitPrice(100);
        var request = new SupplierInvoiceRequest(); request.setPurchaseId(3L); request.setSupplierId(2L);
        request.setInvoiceNumber("INV-1"); request.setItems(List.of(line)); return request;
    }
    @Test void mismatchedSupplierCannotCreateLinkedInvoice() {
        Supplier wrong = mock(Supplier.class); when(wrong.getId()).thenReturn(9L); purchase.setSupplier(wrong);
        assertThrows(BusinessRuleException.class, () -> service.createInvoice(request()));
        verify(invoices, never()).save(any()); verify(items, never()).save(any());
    }
    @Test void validSupplierCanCreateLinkedInvoice() {
        var invoice = service.createInvoice(request()); assertSame(purchase, invoice.getPurchase()); assertEquals(0, new java.math.BigDecimal("200").compareTo(invoice.getTotalAmount()));
        assertEquals(InvoiceStatus.RECEIVED, invoice.getStatus());
    }
    @Test void otherShopPurchaseCannotBeLinked() {
        when(purchases.findByIdAndOrganizationId(3L, 1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.createInvoice(request())); verify(invoices, never()).save(any());
    }
    @ParameterizedTest @ValueSource(strings = {"MATCHED", "MISMATCHED"})
    void matchStatusRequiresReconciliation(String status) {
        when(invoices.findByIdAndOrganizationId(5L, 1L)).thenReturn(Optional.of(new SupplierInvoice()));
        assertThrows(BusinessRuleException.class, () -> service.updateStatus(5L, status)); verify(invoices, never()).save(any());
    }
    @ParameterizedTest @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY})
    void nonFiniteInvoiceQuantitiesRejected(double quantity) {
        var request = request(); request.getItems().get(0).setQuantity(quantity);
        assertThrows(RuntimeException.class, () -> service.createInvoice(request)); verify(items, never()).save(any()); verify(invoices, never()).save(any());
    }
}
