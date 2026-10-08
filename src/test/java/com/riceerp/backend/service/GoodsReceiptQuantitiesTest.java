package com.riceerp.backend.service;

import com.riceerp.backend.dto.*;
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

class GoodsReceiptQuantitiesTest {
    final GoodsReceiptRepository receipts = mock(GoodsReceiptRepository.class);
    final GoodsReceiptItemRepository receiptItems = mock(GoodsReceiptItemRepository.class);
    final PurchaseRepository purchases = mock(PurchaseRepository.class);
    final PurchaseItemRepository lines = mock(PurchaseItemRepository.class);
    final ProductRepository products = mock(ProductRepository.class);
    final StockMovementService movements = mock(StockMovementService.class);
    final ProcurementLock lock = mock(ProcurementLock.class);
    final GoodsReceiptService service = new GoodsReceiptService(receipts, receiptItems, purchases, lines, products, movements, lock);
    final Product product = new Product();
    final Purchase purchase = new Purchase();
    @BeforeEach void setup() {
        when(lock.acquire()).thenReturn(1L); product.setId(2L); product.setStock(0); product.setProductName("Rice");
        purchase.setId(3L); purchase.setStatus(PurchaseStatus.ORDERED);
        when(purchases.findByIdAndOrganizationId(3L, 1L)).thenReturn(Optional.of(purchase));
        when(lines.findByPurchaseId(3L)).thenReturn(List.of(ordered(4), ordered(6)));
        when(receipts.save(any())).thenAnswer(call -> { GoodsReceipt r = call.getArgument(0); r.setId(4L); return r; });
    }
    PurchaseItem ordered(double qty) {
        var line = new PurchaseItem(); line.setProduct(product); line.setQuantity(qty); line.setPrice(100); return line;
    }
    GoodsReceiptRequest request(double... quantities) {
        List<GoodsReceiptItemRequest> items = new ArrayList<>();
        for (double qty : quantities) {
            var line = new GoodsReceiptItemRequest(); line.setProductId(2L); line.setReceivedQty(qty); items.add(line);
        }
        var request = new GoodsReceiptRequest(); request.setItems(items); return request;
    }
    void previouslyReceived(double... quantities) {
        var receipt = new GoodsReceipt(); receipt.setId(5L);
        when(receipts.findByPurchaseId(3L)).thenReturn(List.of(receipt));
        List<GoodsReceiptItem> items = new ArrayList<>();
        for (double qty : quantities) { var line = new GoodsReceiptItem(); line.setProduct(product); line.setReceivedQty(qty); items.add(line); }
        when(receiptItems.findByReceiptId(5L)).thenReturn(items);
    }
    @Test void duplicateOrderLinesAreSummedForReceiptAndCompletion() {
        service.createReceipt(3L, request(10)); assertEquals(PurchaseStatus.RECEIVED, purchase.getStatus());
        assertEquals(10, product.getStock()); verify(movements).record(product, MovementType.PURCHASE_RECEIPT, 10, 4L);
    }
    @Test void receivingLargestLineAloneDoesNotCompleteCombinedOrder() {
        service.createReceipt(3L, request(6)); assertEquals(PurchaseStatus.PARTIALLY_RECEIVED, purchase.getStatus());
    }
    @Test void repeatedReceiptLinesCannotExceedOrder() {
        assertThrows(BusinessRuleException.class, () -> service.createReceipt(3L, request(6, 6)));
        verify(receipts, never()).save(any()); verify(receiptItems, never()).save(any()); verify(products, never()).save(any());
        verifyNoInteractions(movements);
    }
    @Test void repeatedReceiptLinesProduceOneAggregateStockMovement() {
        service.createReceipt(3L, request(4, 6)); assertEquals(10, product.getStock());
        verify(products, times(1)).save(product); verify(receiptItems, times(2)).save(any());
        verify(movements).record(product, MovementType.PURCHASE_RECEIPT, 10, 4L);
    }
    @Test void earlierReceiptsReduceOutstandingQuantity() {
        previouslyReceived(6); assertThrows(BusinessRuleException.class, () -> service.createReceipt(3L, request(5)));
        verify(receipts, never()).save(any());
    }
    @Test void decimalReceiptsSumExactlyAndCompleteOrder() {
        when(lines.findByPurchaseId(3L)).thenReturn(List.of(ordered(0.3))); previouslyReceived(0.1); product.setStock(0.1);
        service.createReceipt(3L, request(0.2)); assertEquals(0.3, product.getStock());
        assertEquals(PurchaseStatus.RECEIVED, purchase.getStatus());
    }
    @Test void receivedHistoryDoesNotExposeFloatingPointAccumulation() {
        previouslyReceived(0.1, 0.2); assertEquals(0.3, service.getReceivedQuantities(3L).get(2L));
    }
    @ParameterizedTest @ValueSource(doubles = {-1, 0, Double.NaN, Double.POSITIVE_INFINITY})
    void invalidReceiptDoesNotWriteAnything(double qty) {
        assertThrows(BusinessRuleException.class, () -> service.createReceipt(3L, request(qty)));
        verify(receipts, never()).save(any()); verifyNoInteractions(movements);
    }
}
