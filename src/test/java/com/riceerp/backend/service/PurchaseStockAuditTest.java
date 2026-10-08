package com.riceerp.backend.service;

import com.riceerp.backend.dto.*;
import com.riceerp.backend.entity.*;
import com.riceerp.backend.enums.*;
import com.riceerp.backend.exception.BusinessRuleException;
import com.riceerp.backend.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PurchaseStockAuditTest {
    final PurchaseRepository purchases = mock(PurchaseRepository.class);
    final PurchaseItemRepository lines = mock(PurchaseItemRepository.class);
    final PurchaseReturnRepository returns = mock(PurchaseReturnRepository.class);
    final ProductRepository products = mock(ProductRepository.class);
    final GoodsReceiptService receipts = mock(GoodsReceiptService.class);
    final StockMovementService movements = mock(StockMovementService.class);
    final ProcurementLock lock = mock(ProcurementLock.class);
    final PurchaseService service = new PurchaseService(purchases, lines, returns, mock(SupplierRepository.class), products, movements, receipts, lock);
    final Product product = new Product();
    final Purchase purchase = new Purchase();

    @BeforeEach void setup() {
        when(lock.acquire()).thenReturn(1L);
        product.setId(2L); product.setProductName("Rice"); product.setStock(100);
        purchase.setId(3L); purchase.setStatus(PurchaseStatus.RECEIVED);
        when(purchases.findByIdAndOrganizationId(3L, 1L)).thenReturn(Optional.of(purchase));
        when(products.findForStockUpdate(2L)).thenReturn(Optional.of(product));
        PurchaseItem line = new PurchaseItem(); line.setProduct(product); line.setQuantity(10);
        when(lines.findByPurchaseId(3L)).thenReturn(List.of(line));
        when(receipts.getReceivedQuantities(3L)).thenReturn(Map.of(2L, 10.0));
        when(returns.save(any())).thenAnswer(call -> call.getArgument(0));
        when(purchases.save(any())).thenAnswer(call -> call.getArgument(0));
    }
    PurchaseReturnRequest request(double qty) {
        var request = new PurchaseReturnRequest(); request.setProductId(2L); request.setQuantityReturned(qty);
        request.setReason("Damaged bags"); return request;
    }
    void returned(double qty) {
        var previous = new PurchaseReturn(); previous.setProduct(product); previous.setQuantityReturned(qty);
        when(returns.findByPurchaseIdAndOrganizationId(3L, 1L)).thenReturn(List.of(previous));
    }
    void rejected(double qty) {
        assertThrows(BusinessRuleException.class, () -> service.createPurchaseReturn(3L, request(qty)));
        verify(products, never()).save(any()); verify(returns, never()).save(any()); verifyNoInteractions(movements);
    }
    @Test void unrelatedProductCannotBeReturnedEvenWithStock() {
        var other = new Product(); other.setId(9L);
        var line = new PurchaseItem(); line.setProduct(other); line.setQuantity(10);
        when(lines.findByPurchaseId(3L)).thenReturn(List.of(line)); rejected(1);
    }
    @Test void orderedButUnreceivedProductCannotBeReturned() {
        when(receipts.getReceivedQuantities(3L)).thenReturn(Map.of()); rejected(1);
    }
    @Test void shopStockDoesNotPermitReturningMoreThanThisPurchaseReceived() { rejected(11); }
    @Test void earlierReturnsReduceRemainingReturnableQuantity() { returned(6); rejected(5); }
    @Test void fullyReturnedPurchaseCannotReturnAgain() { returned(10); rejected(1); }
    @Test void availableStockIsAlsoRequired() { product.setStock(2); rejected(3); }
    @ParameterizedTest @ValueSource(doubles = {-1, 0, Double.NaN, Double.POSITIVE_INFINITY})
    void invalidReturnQuantityCannotChangeStock(double qty) { rejected(qty); }
    @ParameterizedTest @EnumSource(value = PurchaseStatus.class, names = {"DRAFT", "PENDING_APPROVAL", "APPROVED", "ORDERED", "CANCELLED"})
    void unreceivedWorkflowStatesRejectReturns(PurchaseStatus status) { purchase.setStatus(status); rejected(1); }
    @ParameterizedTest @EnumSource(value = PurchaseStatus.class, names = {"PARTIALLY_RECEIVED", "RECEIVED", "COMPLETED"})
    void validReturnReducesStockAndWritesLedger(PurchaseStatus status) {
        purchase.setStatus(status); returned(6);
        PurchaseReturn result = service.createPurchaseReturn(3L, request(4));
        assertEquals(96, product.getStock()); assertSame(purchase, result.getPurchase());
        assertEquals(4, result.getQuantityReturned()); assertEquals("Damaged bags", result.getReason());
        verify(movements).record(product, MovementType.RETURN, -4, 3L);
        assertEquals(status, purchase.getStatus()); // A return does not erase receipt history.
    }
    @Test void fractionalReturnsCanUseExactRemainingAmount() {
        when(receipts.getReceivedQuantities(3L)).thenReturn(Map.of(2L, 0.3)); returned(0.1); product.setStock(0.2);
        service.createPurchaseReturn(3L, request(0.2)); assertEquals(0, product.getStock());
    }
    @Test void otherShopPurchaseIsRejected() {
        when(purchases.findByIdAndOrganizationId(3L, 1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.createPurchaseReturn(3L, request(1)));
        verify(products, never()).save(any()); verifyNoInteractions(movements);
    }
    @Test void completionRejectsLegacyReceivedStatusWithoutReceipts() {
        when(receipts.getReceivedQuantities(3L)).thenReturn(Map.of());
        assertThrows(BusinessRuleException.class, () -> service.updateStatus(3L, PurchaseStatus.COMPLETED));
        verify(purchases, never()).save(any());
    }
    @Test void completionChecksCombinedDuplicateOrderLines() {
        var duplicate = new PurchaseItem(); duplicate.setProduct(product); duplicate.setQuantity(10);
        when(lines.findByPurchaseId(3L)).thenReturn(List.of(duplicate, duplicate));
        assertThrows(BusinessRuleException.class, () -> service.updateStatus(3L, PurchaseStatus.COMPLETED));
    }
    @Test void cancellationRejectsReceiptsEvenIfLegacyStatusSaysOrdered() {
        purchase.setStatus(PurchaseStatus.ORDERED);
        when(receipts.listReceiptsForPurchase(3L)).thenReturn(List.of(new GoodsReceipt()));
        assertThrows(BusinessRuleException.class, () -> service.cancel(3L)); verify(purchases, never()).save(any());
    }
    @Test void cancellationBeforeReceivingRemainsAllowed() {
        purchase.setStatus(PurchaseStatus.ORDERED); assertEquals(PurchaseStatus.CANCELLED, service.cancel(3L).getStatus());
    }
    @Test void positiveOpeningStockIsAudited() {
        when(products.save(any())).thenAnswer(call -> { Product p = call.getArgument(0); p.setId(7L); return p; });
        var productService = new ProductService(products, mock(PriceHistoryRepository.class), movements);
        var request = new ProductRequest(); request.setStock(25); request.setSellingPrice(100);
        var result = productService.createProduct(request);
        verify(movements).record(result, MovementType.OPENING_STOCK, 25, 7L);
    }
    @Test void zeroOpeningStockDoesNotInventMovement() {
        when(products.save(any())).thenAnswer(call -> call.getArgument(0));
        var request = new ProductRequest(); request.setSellingPrice(100);
        new ProductService(products, mock(PriceHistoryRepository.class), movements).createProduct(request);
        verifyNoInteractions(movements);
    }
    @ParameterizedTest @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY})
    void invalidOpeningStockRejectedBeforeWrites(double stock) {
        var request = new ProductRequest(); request.setStock(stock); request.setSellingPrice(100);
        var prices = mock(PriceHistoryRepository.class);
        assertThrows(BusinessRuleException.class, () -> new ProductService(products, prices, movements).createProduct(request));
        verify(products, never()).save(any()); verifyNoInteractions(movements, prices);
    }
}
