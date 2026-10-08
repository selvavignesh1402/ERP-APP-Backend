package com.riceerp.backend.service;

import com.riceerp.backend.dto.*;
import com.riceerp.backend.entity.*;
import com.riceerp.backend.enums.PurchaseStatus;
import com.riceerp.backend.exception.BusinessRuleException;
import com.riceerp.backend.repository.*;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReceiptPriceTest {
    final GoodsReceiptRepository receipts = mock(GoodsReceiptRepository.class);
    final GoodsReceiptItemRepository items = mock(GoodsReceiptItemRepository.class);
    final PurchaseRepository purchases = mock(PurchaseRepository.class);
    final PurchaseItemRepository lines = mock(PurchaseItemRepository.class);
    final ProductRepository products = mock(ProductRepository.class);
    final StockMovementService movements = mock(StockMovementService.class);
    final ProcurementLock lock = mock(ProcurementLock.class);
    final GoodsReceiptService service = new GoodsReceiptService(receipts, items, purchases, lines, products, movements, lock);
    final BigDecimal exact = new BigDecimal("123456789012345.6789");
    final Product product = new Product();

    @BeforeEach void setup() {
        when(lock.acquire()).thenReturn(1L);
        var purchase = new Purchase(); purchase.setId(3L); purchase.setStatus(PurchaseStatus.ORDERED);
        when(purchases.findByIdAndOrganizationId(3L, 1L)).thenReturn(Optional.of(purchase));
        product.setId(2L); product.setStock(10);
        var line = new PurchaseItem(); line.setProduct(product); line.setQuantity(2); line.setPrice(exact);
        when(lines.findByPurchaseId(3L)).thenReturn(List.of(line));
        when(receipts.save(any())).thenAnswer(call -> { GoodsReceipt receipt = call.getArgument(0); receipt.setId(4L); return receipt; });
    }
    GoodsReceiptItemRequest line(BigDecimal price) {
        var line = new GoodsReceiptItemRequest(); line.setProductId(2L); line.setReceivedQty(1); line.setUnitPrice(price); return line;
    }
    GoodsReceiptRequest request(GoodsReceiptItemRequest... lines) {
        var request = new GoodsReceiptRequest(); request.setItems(Arrays.asList(lines)); return request;
    }
    @Test void receiptKeepsExactSpecifiedPrice() {
        service.createReceipt(3L, request(line(exact)));
        verify(items).save(argThat(item -> exact.equals(item.getUnitPrice())));
        assertEquals(11, product.getStock());
    }
    @Test void internalZeroPriceFallbackKeepsExactPurchasePrice() {
        service.createReceipt(3L, request(line(BigDecimal.ZERO)));
        verify(items).save(argThat(item -> exact.equals(item.getUnitPrice())));
    }
    @Test void invalidLaterPriceDoesNotWriteReceiptOrChangeStock() {
        for (BigDecimal bad : new BigDecimal[]{null, new BigDecimal("-1"), new BigDecimal("1000000000000000"), new BigDecimal("1.00001")}) {
            assertThrows(BusinessRuleException.class, () -> service.createReceipt(3L, request(line(exact), line(bad))));
        }
        verify(receipts, never()).save(any()); verify(items, never()).save(any());
        verify(products, never()).save(any()); verify(purchases, never()).save(any());
        verifyNoInteractions(movements); assertEquals(10, product.getStock());
    }
}
