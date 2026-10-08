package com.riceerp.backend.service;

import com.riceerp.backend.dto.ProductRequest;
import com.riceerp.backend.repository.*;
import com.riceerp.backend.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductGstTest {
    @Test void noGstIsStoredEmptyAndSupportedRateIsRetained() {
        var products = mock(ProductRepository.class);
        when(products.save(any())).thenAnswer(call -> call.getArgument(0));
        var service = new ProductService(products, mock(PriceHistoryRepository.class), mock(StockMovementService.class));
        var request = new ProductRequest();
        request.setProductName("Test"); request.setCategory("Other"); request.setSellingPrice(100);
        assertNull(service.createProduct(request).getGstRate());
        request.setGstRate(0.0);
        assertNull(service.createProduct(request).getGstRate());
        request.setGstRate(18.0);
        assertEquals(18.0, service.createProduct(request).getGstRate());
    }
    @Test void unsupportedRateIsRejectedBeforeAnyWrite() {
        var products = mock(ProductRepository.class);
        var prices = mock(PriceHistoryRepository.class);
        var movements = mock(StockMovementService.class);
        var service = new ProductService(products, prices, movements);
        var request = new ProductRequest(); request.setGstRate(7.0); request.setSellingPrice(100);
        assertThrows(BusinessRuleException.class, () -> service.createProduct(request));
        assertThrows(BusinessRuleException.class, () -> service.updateProduct(1L, request));
        verifyNoInteractions(products, prices, movements);
    }
}
