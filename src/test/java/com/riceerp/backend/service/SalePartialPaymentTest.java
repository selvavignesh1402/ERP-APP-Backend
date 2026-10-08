package com.riceerp.backend.service;

import com.riceerp.backend.dto.*;
import com.riceerp.backend.entity.*;
import com.riceerp.backend.enums.*;
import com.riceerp.backend.exception.BusinessRuleException;
import com.riceerp.backend.repository.*;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.mockito.ArgumentCaptor;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SalePartialPaymentTest {
    final SaleRepository sales = mock(SaleRepository.class);
    final SaleItemRepository items = mock(SaleItemRepository.class);
    final ProductRepository products = mock(ProductRepository.class);
    final PaymentRepository payments = mock(PaymentRepository.class);
    final CustomerRepository customers = mock(CustomerRepository.class);
    final StockMovementService movements = mock(StockMovementService.class);
    final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    final SaleService service = new SaleService(sales, items, products, payments, customers, movements, transactions);
    final Customer customer = new Customer();
    SaleRequest request;

    @BeforeEach void setup() {
        when(payments.sumByReference(any(), any())).thenReturn(BigDecimal.ZERO);
        when(payments.sumAllocatedToSale(any())).thenReturn(BigDecimal.ZERO);
        Product product = new Product(); product.setGstRate(5.0); product.setId(1L); product.setStock(10); product.setSellingPrice(1000);
        when(products.findForStockUpdate(1L)).thenReturn(Optional.of(product));
        customer.setCreditBalance(200); customer.setCreditLimit(1000);
        when(customers.findById(2L)).thenReturn(Optional.of(customer));
        when(sales.save(any())).thenAnswer(invocation -> { Sale sale = invocation.getArgument(0); if (sale == null) return null; sale.setId(3L); return sale; });
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        SaleItemRequest line = new SaleItemRequest(); line.setProductId(1L); line.setQuantity(1); line.setPrice(1000);
        request = new SaleRequest(); request.setItems(List.of(line)); request.setPaymentMode("CREDIT");
        request.setCustomerId(2L); request.setPaidAmount(new BigDecimal("400.0")); request.setInitialPaymentMode("UPI");
    }
    @Test void partialPaymentCreatesAllocationAndOnlyRemainingDebt() {
        Sale sale = service.createSale(request);
        assertEquals(0, new java.math.BigDecimal("1050").compareTo(sale.getGrandTotal())); assertEquals(0, new java.math.BigDecimal("400").compareTo(sale.getPaidAmount()));
        assertEquals(0, new java.math.BigDecimal("650").compareTo(sale.getBalanceDue())); assertEquals(0, new BigDecimal("850").compareTo(customer.getCreditBalance()));
        ArgumentCaptor<Payment> capture = ArgumentCaptor.forClass(Payment.class); verify(payments).save(capture.capture());
        assertEquals(ReferenceType.SALE, capture.getValue().getReferenceType());
        assertEquals(3L, capture.getValue().getReferenceId()); assertEquals(0, new BigDecimal("400").compareTo(capture.getValue().getAmount()));
        assertEquals(PaymentMode.UPI, capture.getValue().getPaymentMode());
    }
    @Test void readsReconstructTotalsFromSavedPaymentsIncludingLaterCollections() {
        Sale persisted = new Sale(); persisted.setId(3L); persisted.setGrandTotal(1050); persisted.setPaymentMode(PaymentMode.CREDIT);
        when(sales.findById(3L)).thenReturn(Optional.of(persisted)); when(sales.findAll()).thenReturn(List.of(persisted));
        when(payments.sumByReference(ReferenceType.SALE, 3L)).thenReturn(BigDecimal.valueOf(500.0));
        when(payments.sumAllocatedToSale(3L)).thenReturn(BigDecimal.valueOf(100.0));
        assertEquals(0, new java.math.BigDecimal("600").compareTo(service.getSaleById(3L).getPaidAmount()));
        assertEquals(0, new java.math.BigDecimal("450").compareTo(service.listSales().get(0).getBalanceDue()));
    }
    @ParameterizedTest @ValueSource(strings = {"-1", "1051", "1000000000000000", "400.001", "1E+30"})
    void invalidAmountsRejectedBeforeWrites(String amount) {
        request.setPaidAmount(new BigDecimal(amount));
        assertThrows(BusinessRuleException.class, () -> service.createSale(request));
        verify(sales, never()).save(any()); verify(payments, never()).save(any());
        verify(customers, never()).save(any()); assertEquals(0, new BigDecimal("200").compareTo(customer.getCreditBalance()));
    }
    @Test void initialCollectionRequiresRealPaymentMode() {
        request.setInitialPaymentMode("CREDIT");
        assertThrows(BusinessRuleException.class, () -> service.createSale(request));
        request.setInitialPaymentMode(null);
        assertThrows(BusinessRuleException.class, () -> service.createSale(request));
        verify(sales, never()).save(any());
    }
    @Test void partialPaymentRequiresRegisteredCustomer() {
        request.setCustomerId(null);
        assertThrows(BusinessRuleException.class, () -> service.createSale(request)); verify(payments, never()).save(any());
    }
    @Test void remainingAmountMustFitCreditLimit() {
        customer.setCreditLimit(849);
        assertThrows(BusinessRuleException.class, () -> service.createSale(request));
        verify(sales, never()).save(any()); assertEquals(0, new BigDecimal("200").compareTo(customer.getCreditBalance()));
    }
    @Test void oneCentRemainingDebtFitsExactLargeCreditLimit() {
        customer.setCreditBalance(new BigDecimal("123456789012345.67"));
        customer.setCreditLimit(new BigDecimal("123456789012345.68"));
        request.setPaidAmount(new BigDecimal("1049.99"));
        service.createSale(request);
        assertEquals(new BigDecimal("123456789012345.68"), customer.getCreditBalance());
    }

    @Test void oneCentOverLargeCreditLimitIsRejectedWithoutWrites() {
        customer.setCreditBalance(new BigDecimal("123456789012345.67"));
        customer.setCreditLimit(new BigDecimal("123456789012345.67"));
        request.setPaidAmount(new BigDecimal("1049.99"));
        assertThrows(BusinessRuleException.class, () -> service.createSale(request));
        verify(sales, never()).save(any());
        verify(payments, never()).save(any());
        assertEquals(new BigDecimal("123456789012345.67"), customer.getCreditBalance());
    }

    @Test void legacyCreditWithoutPaidAmountStillCreatesFullDebt() {
        customer.setCreditLimit(0); request.setPaidAmount(null); request.setInitialPaymentMode(null);
        Sale sale = service.createSale(request);
        assertEquals(0, new java.math.BigDecimal("0").compareTo(sale.getPaidAmount())); assertEquals(0, new java.math.BigDecimal("1050").compareTo(sale.getBalanceDue()));
        assertEquals(0, new BigDecimal("1250").compareTo(customer.getCreditBalance())); verify(payments, never()).save(any());
    }
    @Test void largeInvoiceRetainsOneCentDiscountThroughTaxAndCashPayment() {
        request.getItems().get(0).setPrice(100000000000000.0);
        request.setDiscount(new BigDecimal("0.01"));
        request.setPaymentMode("CASH");
        request.setPaidAmount(null);
        Sale sale = service.createSale(request);
        var expected = new BigDecimal("104999999999999.99");
        assertEquals(new BigDecimal("100000000000000.00"), sale.getTotal());
        assertEquals(new BigDecimal("0.01"), sale.getDiscount());
        assertEquals(expected, sale.getGrandTotal());
        assertEquals(expected, sale.getPaidAmount());
        assertEquals(0, sale.getBalanceDue().signum());
        var payment = ArgumentCaptor.forClass(Payment.class);
        verify(payments).save(payment.capture());
        assertEquals(expected, payment.getValue().getAmount());
    }

    @Test void readingLargeInvoiceRetainsOneCentOutstanding() {
        Sale sale = new Sale();
        sale.setId(3L);
        sale.setGrandTotal(new BigDecimal("123456789012345.68"));
        when(sales.findById(3L)).thenReturn(Optional.of(sale));
        when(payments.sumByReference(ReferenceType.SALE, 3L)).thenReturn(new BigDecimal("123456789012345.67"));
        Sale loaded = service.getSaleById(3L);
        assertEquals(new BigDecimal("0.01"), loaded.getBalanceDue());
        assertEquals(new BigDecimal("123456789012345.67"), loaded.getPaidAmount());
    }

    @Test void exactLargeDiscountLeavesOneCentSale() {
        request.getItems().get(0).setPrice(100000000000000.0);
        request.setDiscount(new BigDecimal("99999999999999.99"));
        request.setPaymentMode("CASH");
        request.setPaidAmount(null);
        Sale sale = service.createSale(request);
        assertEquals(new BigDecimal("0.01"), sale.getGrandTotal());
        assertEquals(new BigDecimal("99999999999999.99"), sale.getDiscount());
        assertEquals(new BigDecimal("0.01"), sale.getPaidAmount());
    }

    @Test void offlineInitialPaymentRetainsOneCentDebtAndRetryDoesNotAddItAgain() throws Exception {
        customer.setCreditLimit(0);
        var offline = new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                "{\"clientReferenceId\":\"large-offline\",\"customerId\":2,\"paymentMode\":\"CREDIT\","
                + "\"initialPaymentMode\":\"CASH\",\"paidAmount\":104999999999999.98,\"discount\":0.01,"
                + "\"items\":[{\"productId\":1,\"quantity\":1,\"price\":100000000000000}]}",
                OfflineSaleSyncRequest.class);
        var result = service.syncBatchSales(List.of(offline));
        assertEquals(1, result.getSuccessCount());
        Sale sale = result.getResults().get(0).getSale();
        assertEquals(new BigDecimal("104999999999999.98"), sale.getPaidAmount());
        assertEquals(new BigDecimal("0.01"), sale.getBalanceDue());
        assertEquals(new BigDecimal("200.01"), customer.getCreditBalance());
        when(sales.findByClientReferenceId("large-offline")).thenReturn(Optional.of(sale));
        when(payments.sumByReference(ReferenceType.SALE, 3L)).thenReturn(sale.getPaidAmount());
        assertEquals(1, service.syncBatchSales(List.of(offline)).getDuplicateCount());
        verify(payments, times(1)).save(any());
        verify(customers, times(1)).save(any());
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.NullSource
    @ValueSource(strings = {"-0.01", "1000000000000000", "0.00001"})
    void invalidDiscountsFailBeforeWrites(String discount) {
        request.setDiscount(discount == null ? null : new BigDecimal(discount));
        assertThrows(BusinessRuleException.class, () -> service.createSale(request));
        verify(sales, never()).save(any());
        verify(payments, never()).save(any());
        verify(customers, never()).save(any());
        verify(products, never()).save(any());
    }

    @Test void oversizedCalculatedInvoiceFailsBeforeWrites() {
        request.getItems().get(0).setPrice(999999999999999.0);
        request.setPaidAmount(null);
        request.setPaymentMode("CASH");
        assertThrows(BusinessRuleException.class, () -> service.createSale(request));
        verify(sales, never()).save(any());
        verify(payments, never()).save(any());
        verify(customers, never()).save(any());
        verify(products, never()).save(any());
    }

    @Test void offlineBatchRejectsInvalidDiscountButContinuesWithValidSale() {
        var invalid = new OfflineSaleSyncRequest();
        invalid.setClientReferenceId("invalid-discount");
        invalid.setPaymentMode("CASH");
        invalid.setDiscount(new BigDecimal("-0.01"));
        invalid.setItems(request.getItems());
        var valid = new OfflineSaleSyncRequest();
        valid.setClientReferenceId("valid-sale");
        valid.setPaymentMode("CASH");
        valid.setItems(request.getItems());
        var result = service.syncBatchSales(List.of(invalid, valid));
        assertEquals(1, result.getFailureCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals("FAILED", result.getResults().get(0).getStatus());
        assertTrue(result.getResults().get(0).getErrorMessage().contains("Discount"));
        verify(sales, times(1)).save(any());
        verify(payments, times(1)).save(any());
    }

    @Test void productWithoutGstCreatesUntaxedInvoice() {
        products.findForStockUpdate(1L).orElseThrow().setGstRate(null);
        request.setPaymentMode("CASH"); request.setPaidAmount(null);
        Sale sale = service.createSale(request);
        assertEquals(new BigDecimal("1000.00"), sale.getGrandTotal());
        assertEquals(0, sale.getCgst().signum());
        assertEquals(0, sale.getSgst().signum());
        assertEquals(0, sale.getIgst().signum());
    }
    @Test void deliveryUsesBookedRateAndInterstateTypeAfterProductRateChanges() {
        products.findForStockUpdate(1L).orElseThrow().setGstRate(18.0);
        Sale sale = service.createSaleFromDelivery(9L, 8L, customer, request.getItems(), PaymentMode.CASH, BigDecimal.ZERO,
                TaxType.INTER_STATE, Map.of(1L, 5.0));
        assertEquals(new BigDecimal("50.00"), sale.getIgst());
        assertEquals(0, sale.getCgst().signum());
        assertEquals(new BigDecimal("1050.00"), sale.getGrandTotal());
        var item = ArgumentCaptor.forClass(SaleItem.class);
        verify(items).save(item.capture());
        assertEquals(5.0, item.getValue().getGstRate());
    }
    @Test void deliveryPreservesExplicitNoGstSnapshot() {
        Map<Long, Double> rates = new HashMap<>(); rates.put(1L, null);
        Sale sale = service.createSaleFromDelivery(9L, 8L, customer, request.getItems(), PaymentMode.CASH, BigDecimal.ZERO,
                TaxType.INTER_STATE, rates);
        assertEquals(new BigDecimal("1000.00"), sale.getGrandTotal());
        assertEquals(0, sale.getIgst().signum());
    }
    @Test void deliveryInvoicePreservesTheExactAllocatedDiscount() {
        Map<Long, Double> rates = new HashMap<>(); rates.put(1L, null);
        request.getItems().get(0).setQuantity(1);
        request.getItems().get(0).setPrice(300000000000000.0);
        var discount = new BigDecimal("274485596337448.55");
        Sale sale = service.createSaleFromDelivery(9L, 8L, customer, request.getItems(), PaymentMode.CASH, discount,
                TaxType.INTER_STATE, rates);
        assertEquals(0, discount.compareTo(sale.getDiscount()));
        assertEquals(new BigDecimal("25514403662551.45"), sale.getGrandTotal());
        var payment = ArgumentCaptor.forClass(Payment.class);
        verify(payments).save(payment.capture());
        assertEquals(sale.getGrandTotal(), payment.getValue().getAmount());
    }
    @Test void deliveryInvoicePreservesTheExactAgreedItemPrice() {
        Map<Long, Double> rates = new HashMap<>(); rates.put(1L, null);
        request.getItems().get(0).setQuantity(1);
        var price = new BigDecimal("123456789012345.67");
        request.getItems().get(0).setPrice(price);
        var sale = service.createSaleFromDelivery(9L, 8L, customer, request.getItems(), PaymentMode.CASH,
                BigDecimal.ZERO, TaxType.INTER_STATE, rates);
        assertEquals(price, sale.getGrandTotal());
        var saved = ArgumentCaptor.forClass(SaleItem.class);
        verify(items).save(saved.capture());
        assertEquals(price, saved.getValue().getPrice());
    }
    @Test void staleOfflineGstCannotSilentlyChangeAnInvoice() {
        request.getItems().get(0).setGstRate(18.0);
        assertThrows(BusinessRuleException.class, () -> service.createSale(request));
        verify(sales, never()).save(any()); verify(payments, never()).save(any());
    }

    @Test void cashStillCreatesFullPayment() {
        request.setPaymentMode("CASH"); request.setPaidAmount(null);
        Sale sale = service.createSale(request);
        assertEquals(0, new java.math.BigDecimal("1050").compareTo(sale.getPaidAmount())); assertEquals(0, new java.math.BigDecimal("0").compareTo(sale.getBalanceDue()));
        assertEquals(0, new BigDecimal("200").compareTo(customer.getCreditBalance()));
    }
    @Test void offlineSyncPreservesCollectionAndRetryDoesNotChargeAgain() {
        OfflineSaleSyncRequest offline = new OfflineSaleSyncRequest(); offline.setClientReferenceId("partial");
        offline.setCustomerId(2L); offline.setPaymentMode("CREDIT"); offline.setPaidAmount(new BigDecimal("400.0"));
        offline.setInitialPaymentMode("CARD"); offline.setItems(request.getItems());
        SyncBatchResponse first = service.syncBatchSales(List.of(offline));
        assertEquals(1, first.getSuccessCount());
        Sale saved = first.getResults().get(0).getSale(); assertEquals(0, new java.math.BigDecimal("650").compareTo(saved.getBalanceDue()));
        when(sales.findByClientReferenceId("partial")).thenReturn(Optional.of(saved));
        when(payments.sumByReference(ReferenceType.SALE, 3L)).thenReturn(BigDecimal.valueOf(400.0));
        SyncBatchResponse retry = service.syncBatchSales(List.of(offline));
        assertEquals(1, retry.getDuplicateCount()); assertEquals(0, new java.math.BigDecimal("400").compareTo(retry.getResults().get(0).getSale().getPaidAmount()));
        verify(payments, times(1)).save(any()); assertEquals(0, new BigDecimal("850").compareTo(customer.getCreditBalance()));
    }

    @Test void paymentFailureRollsBackSaleStockAndCustomerBalance() {
        var source = new org.springframework.jdbc.datasource.DriverManagerDataSource(
                "jdbc:h2:mem:partial_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        var jdbc = new org.springframework.jdbc.core.JdbcTemplate(source);
        jdbc.execute("create table ledger_test(kind varchar(30), amount double)");
        jdbc.update("insert into ledger_test values ('balance', 200), ('stock', 10)");
        when(customers.save(any())).thenAnswer(i -> {
            jdbc.update("update ledger_test set amount=? where kind='balance'", ((Customer)i.getArgument(0)).getCreditBalance());
            return i.getArgument(0);
        });
        when(products.save(any())).thenAnswer(i -> {
            jdbc.update("update ledger_test set amount=? where kind='stock'", ((Product)i.getArgument(0)).getStock());
            return i.getArgument(0);
        });
        when(sales.save(any())).thenAnswer(i -> {
            Sale sale = i.getArgument(0); if (sale == null) return null; sale.setId(3L);
            jdbc.update("insert into ledger_test values ('sale', ?)", sale.getGrandTotal()); return sale;
        });
        when(payments.save(any())).thenAnswer(i -> {
            jdbc.update("insert into ledger_test values ('payment', ?)", ((Payment)i.getArgument(0)).getAmount());
            throw new IllegalStateException("Simulated payment persistence failure");
        });
        var manager = new org.springframework.jdbc.datasource.DataSourceTransactionManager(source);
        var proxy = new org.springframework.aop.framework.ProxyFactory(service);
        proxy.addAdvice(new org.springframework.transaction.interceptor.TransactionInterceptor(manager,
                new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()));
        assertThrows(IllegalStateException.class, () -> ((SaleService)proxy.getProxy()).createSale(request));
        assertEquals(0, jdbc.queryForObject("select count(*) from ledger_test where kind in ('sale','payment')", Integer.class));
        assertEquals(200, jdbc.queryForObject("select amount from ledger_test where kind='balance'", Double.class));
        assertEquals(10, jdbc.queryForObject("select amount from ledger_test where kind='stock'", Double.class));
        jdbc.execute("shutdown");
    }
}
