package com.riceerp.backend.service;

import com.riceerp.backend.dto.ProductRequest;
import com.riceerp.backend.entity.*;
import com.riceerp.backend.repository.*;
import com.riceerp.backend.exception.BusinessRuleException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductPriceTest {
    final ProductRepository products = mock(ProductRepository.class);
    final PriceHistoryRepository history = mock(PriceHistoryRepository.class);
    final StockMovementService movements = mock(StockMovementService.class);
    final ProductService service = new ProductService(products, history, movements);
    ProductRequest request() {
        var request = new ProductRequest(); request.setProductName("Rice"); request.setCategory("Other");
        request.setPurchasePrice(new BigDecimal("123456789012345.6788"));
        request.setSellingPrice(new BigDecimal("123456789012345.6789")); return request;
    }
    @Test void exactRequestPricesReachProductAndBothHistoryEntries() throws Exception {
        var request = new ObjectMapper().readValue("{\"productName\":\"Rice\",\"category\":\"Other\",\"purchasePrice\":123456789012345.6788,\"sellingPrice\":123456789012345.6789}", ProductRequest.class);
        when(products.save(any())).thenAnswer(call -> call.getArgument(0));
        var product = service.createProduct(request);
        assertEquals(new BigDecimal("123456789012345.6788"), product.getPurchasePrice());
        assertEquals(new BigDecimal("123456789012345.6789"), product.getSellingPrice());
        var captured = ArgumentCaptor.forClass(PriceHistory.class);
        verify(history, times(2)).save(captured.capture());
        assertEquals(product.getPurchasePrice(), captured.getAllValues().get(0).getPrice());
        assertEquals(product.getSellingPrice(), captured.getAllValues().get(1).getPrice());
    }
    @Test void formattingOnlyDoesNotCreateHistoryButOneTenThousandthDoes() {
        var product = new Product(); product.setPurchasePrice(new BigDecimal("100.0000")); product.setSellingPrice(new BigDecimal("120.0000"));
        when(products.findById(1L)).thenReturn(Optional.of(product));
        when(products.save(any())).thenAnswer(call -> call.getArgument(0));
        var request = request(); request.setPurchasePrice(new BigDecimal("100.00")); request.setSellingPrice(new BigDecimal("120.0"));
        service.updateProduct(1L, request); verifyNoInteractions(history);
        request.setSellingPrice(new BigDecimal("120.0001")); service.updateProduct(1L, request);
        var captured = ArgumentCaptor.forClass(PriceHistory.class); verify(history).save(captured.capture());
        assertEquals(new BigDecimal("120.0001"), captured.getValue().getPrice());
    }
    @Test void invalidPricesAreRejectedBeforeAnyRepositoryWrites() {
        for (String invalid : new String[]{null, "-1", "1000000000000000", "1.00001"}) {
            var price = invalid == null ? null : new BigDecimal(invalid);
            var purchase = request(); purchase.setPurchasePrice(price);
            var selling = request(); selling.setSellingPrice(price);
            for (var request : new ProductRequest[]{purchase, selling}) {
                assertThrows(BusinessRuleException.class, () -> service.createProduct(request));
                assertThrows(BusinessRuleException.class, () -> service.updateProduct(1L, request));
            }
        }
        var zero = request(); zero.setSellingPrice(BigDecimal.ZERO);
        assertThrows(BusinessRuleException.class, () -> service.createProduct(zero));
        verifyNoInteractions(products, history, movements);
    }
    @Test void beanValidationAcceptsTrailingZeroesButRejectsInvalidPrices() throws Exception {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var mapper = new ObjectMapper();
            var valid = mapper.readValue("{\"productName\":\"Rice\",\"category\":\"Other\",\"purchasePrice\":0,\"sellingPrice\":1.230000}", ProductRequest.class);
            assertTrue(factory.getValidator().validate(valid).isEmpty());
            for (String field : new String[]{"purchasePrice", "sellingPrice"})
                for (String value : new String[]{"null", "-1", "1.00001", "1000000000000000"}) {
                    var data = mapper.valueToTree(valid); ((com.fasterxml.jackson.databind.node.ObjectNode)data).set(field, mapper.readTree(value));
                    assertFalse(factory.getValidator().validate(mapper.treeToValue(data, ProductRequest.class)).isEmpty());
                }
        }
    }
}
