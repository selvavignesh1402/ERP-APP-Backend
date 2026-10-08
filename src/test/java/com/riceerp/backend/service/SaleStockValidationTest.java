package com.riceerp.backend.service;

import com.riceerp.backend.dto.*;
import com.riceerp.backend.entity.*;
import com.riceerp.backend.enums.*;
import com.riceerp.backend.exception.BusinessRuleException;
import com.riceerp.backend.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SaleStockValidationTest {
    final SaleRepository sales = mock(SaleRepository.class);
    final SaleItemRepository items = mock(SaleItemRepository.class);
    final ProductRepository products = mock(ProductRepository.class);
    final PaymentRepository payments = mock(PaymentRepository.class);
    final CustomerRepository customers = mock(CustomerRepository.class);
    final StockMovementService movements = mock(StockMovementService.class);
    final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    final SaleService service = new SaleService(sales, items, products, payments, customers, movements, transactions);
    Product product;

    @BeforeEach void setup() {
        when(payments.sumByReference(any(), any())).thenReturn(java.math.BigDecimal.ZERO);
        when(payments.sumAllocatedToSale(any())).thenReturn(java.math.BigDecimal.ZERO);
        product = product(1L, 10);
        when(sales.save(any())).thenAnswer(invocation -> {
            Sale sale = invocation.getArgument(0); sale.setId(50L); return sale;
        });
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    }
    Product product(long id, double stock) {
        Product p = new Product(); p.setGstRate(5.0); p.setId(id); p.setProductName("Product " + id);
        p.setStock(stock); p.setSellingPrice(100); p.setPurchasePrice(50);
        when(products.findForStockUpdate(id)).thenReturn(Optional.of(p)); return p;
    }
    SaleItemRequest line(long id, double quantity, double price) {
        SaleItemRequest line = new SaleItemRequest(); line.setProductId(id); line.setQuantity(quantity); line.setPrice(price);
        return line;
    }
    SaleRequest request(SaleItemRequest... lines) {
        SaleRequest request = new SaleRequest(); request.setPaymentMode("CASH"); request.setItems(Arrays.asList(lines)); return request;
    }
    void noWrites() {
        verify(sales, never()).save(any()); verify(items, never()).save(any());
        verify(products, never()).save(any()); verify(payments, never()).save(any());
        verify(customers, never()).save(any()); verifyNoInteractions(movements);
    }

    @Test void combinedDemandExceedingStockRejectedBeforeWrites() {
        var error = assertThrows(BusinessRuleException.class,
                () -> service.createSale(request(line(1, 6, 100), line(1, 6, 100))));
        assertTrue(error.getMessage().contains("Requested: 12")); assertEquals(10, product.getStock()); noWrites();
    }
    @Test void exactStockDuplicatesPreservePricesAndDeductOnce() {
        Sale sale = service.createSale(request(line(1, 4, 100), line(1, 6, 120)));
        assertEquals(0, product.getStock()); assertEquals(0, new java.math.BigDecimal("1120").compareTo(sale.getTotal()));
        verify(products, times(1)).findForStockUpdate(1L); verify(products, times(1)).save(product);
        verify(movements).record(product, MovementType.SALE, -10, 50L);
        ArgumentCaptor<SaleItem> saved = ArgumentCaptor.forClass(SaleItem.class);
        verify(items, times(2)).save(saved.capture());
        assertEquals(4, saved.getAllValues().get(0).getQuantity());
        assertEquals(0, new java.math.BigDecimal("100").compareTo(saved.getAllValues().get(0).getPrice()));
        assertEquals(6, saved.getAllValues().get(1).getQuantity());
        assertEquals(0, new java.math.BigDecimal("120").compareTo(saved.getAllValues().get(1).getPrice()));
    }
    @Test void nonAdjacentDuplicateLinesAreCombined() {
        Product second = product(2, 9);
        service.createSale(request(line(1, 2, 100), line(2, 3, 100), line(1, 3, 100)));
        assertEquals(5, product.getStock()); assertEquals(6, second.getStock());
        verify(movements).record(product, MovementType.SALE, -5, 50L);
        verify(movements).record(second, MovementType.SALE, -3, 50L);
        verify(products, times(2)).save(any());
    }
    @Test void laterProductShortageDoesNotDeductEarlierProduct() {
        Product second = product(2, 5);
        assertThrows(BusinessRuleException.class, () -> service.createSale(request(
                line(1, 2, 100), line(2, 3, 100), line(2, 3, 100))));
        assertEquals(10, product.getStock()); assertEquals(5, second.getStock()); noWrites();
    }
    @Test void fractionalExactStockDoesNotFailFromBinaryRounding() {
        product.setStock(0.3);
        service.createSale(request(line(1, 0.1, 100), line(1, 0.2, 100)));
        assertEquals(0, product.getStock()); verify(movements).record(product, MovementType.SALE, -0.3, 50L);
    }
    @ParameterizedTest @ValueSource(doubles = {0, -1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void invalidQuantityCannotBypassStockValidation(double quantity) {
        assertThrows(BusinessRuleException.class, () -> service.createSale(request(line(1, quantity, 100))));
        assertEquals(10, product.getStock()); noWrites();
    }
    @Test void combinedQuantityOverflowCannotBypassStockValidation() {
        product.setStock(Double.MAX_VALUE);
        assertThrows(BusinessRuleException.class, () -> service.createSale(request(
                line(1, Double.MAX_VALUE, 100), line(1, Double.MAX_VALUE, 100))));
        assertEquals(Double.MAX_VALUE, product.getStock()); noWrites();
    }
    @Test void fractionalLineTotalsKeepExactCentBoundaryBeforeTax() {
        product.setPurchasePrice(0);
        Sale sale = service.createSale(request(line(1, 1, 0.1), line(1, 1, 0.2)));
        assertEquals(0, new java.math.BigDecimal("0.3").compareTo(sale.getTotal()));
        assertEquals(0, new java.math.BigDecimal("0.01").compareTo(sale.getCgst()));
        assertEquals(0, new java.math.BigDecimal("0.01").compareTo(sale.getSgst()));
        assertEquals(0, new java.math.BigDecimal("0.32").compareTo(sale.getGrandTotal()));
    }

    @Test void singleLineSalesStillWork() {
        service.createSale(request(line(1, 3, 100)));
        assertEquals(7, product.getStock()); verify(items).save(any());
        verify(movements).record(product, MovementType.SALE, -3, 50L);
    }
    @Test void catalogDefaultPriceDoesNotRoundThroughDouble() {
        var exact = new java.math.BigDecimal("123456789012345.67");
        product.setSellingPrice(exact); product.setGstRate(null);
        var result = service.createSale(request(line(1, 1, 0)));
        assertEquals(exact, result.getGrandTotal());
        var saved = ArgumentCaptor.forClass(SaleItem.class); verify(items).save(saved.capture());
        assertEquals(exact, saved.getValue().getPrice());
    }
    @Test void tinyDifferenceBelowLargeCostCannotPassTheCostCheck() {
        product.setPurchasePrice(new java.math.BigDecimal("123456789012345.6789"));
        var line = line(1, 1, 100); line.setPrice(new java.math.BigDecimal("123456789012345.6788"));
        assertThrows(BusinessRuleException.class, () -> service.createSale(request(line)));
        noWrites();
    }
    @Test void offlineBatchUsesSameCombinedStockValidation() {
        OfflineSaleSyncRequest offline = new OfflineSaleSyncRequest();
        offline.setClientReferenceId("stock-test"); offline.setPaymentMode("CASH");
        offline.setItems(List.of(line(1, 6, 100), line(1, 6, 100)));
        SyncBatchResponse result = service.syncBatchSales(List.of(offline));
        assertEquals(1, result.getFailureCount()); assertEquals(0, result.getSuccessCount());
        assertEquals(10, product.getStock()); noWrites(); verify(transactions).rollback(any());
    }
    @Test void deliveryInvoiceUsesSameCombinedStockValidation() {
        assertThrows(BusinessRuleException.class, () -> service.createSaleFromDelivery(1L, 2L, null,
                List.of(line(1, 6, 100), line(1, 6, 100)), PaymentMode.CASH, 0));
        assertEquals(10, product.getStock()); noWrites();
    }
    @Test void alreadySyncedSaleDoesNotDeductAgain() {
        Sale existing = new Sale();
        when(sales.findByClientReferenceId("existing")).thenReturn(Optional.of(existing));
        assertSame(existing, service.createSaleInternal(request(line(1, 6, 100), line(1, 6, 100)), "existing", null));
        assertEquals(10, product.getStock()); noWrites(); verifyNoInteractions(products);
    }
    @Test void deliveryInvoiceRetriesUseStableReferenceWithoutNewStockOrPayment() {
        Sale existing = new Sale(); existing.setId(70L); existing.setDeliveryId(4L); existing.setSalesOrderId(3L);
        when(sales.findByClientReferenceId("DELIVERY-4")).thenReturn(Optional.of(existing));
        assertSame(existing, service.createSaleFromDelivery(4L, 3L, null, List.of(line(1, 1, 100)), PaymentMode.CASH, 0));
        verifyNoInteractions(products, items, movements); verify(payments, never()).save(any());
        assertEquals(10, product.getStock());
    }

    @Test void concurrentCommittedDuplicateIsReturnedAfterRollback() {
        Sale existing = new Sale(); existing.setId(70L); existing.setBillNumber("committed");
        when(sales.findByClientReferenceId("retry")).thenReturn(Optional.empty(), Optional.empty(), Optional.of(existing));
        doThrow(new org.springframework.dao.DataIntegrityViolationException("Duplicate reference")).when(sales).save(any());
        OfflineSaleSyncRequest offline = new OfflineSaleSyncRequest();
        offline.setClientReferenceId("retry"); offline.setPaymentMode("CASH"); offline.setItems(List.of(line(1, 1, 100)));
        SyncBatchResponse result = service.syncBatchSales(List.of(offline));
        assertEquals(1, result.getDuplicateCount()); assertEquals(0, result.getFailureCount());
        assertEquals("ALREADY_SYNCED", result.getResults().get(0).getStatus());
        assertEquals(70L, result.getResults().get(0).getServerSaleId());
        verify(transactions).rollback(any()); verifyNoInteractions(movements);
    }

    @ParameterizedTest @ValueSource(strings = {"", " ", "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz"})
    void invalidOfflineReferenceCannotCreateSale(String reference) {
        OfflineSaleSyncRequest offline = new OfflineSaleSyncRequest();
        offline.setClientReferenceId(reference); offline.setPaymentMode("CASH"); offline.setItems(List.of(line(1, 1, 100)));
        assertEquals(1, service.syncBatchSales(List.of(offline)).getFailureCount()); noWrites();
    }
}
