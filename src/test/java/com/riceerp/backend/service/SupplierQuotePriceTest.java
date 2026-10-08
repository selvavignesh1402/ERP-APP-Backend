package com.riceerp.backend.service;

import com.riceerp.backend.dto.SupplierProductRequest;
import com.riceerp.backend.entity.*;
import com.riceerp.backend.repository.*;
import com.riceerp.backend.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SupplierQuotePriceTest {
    final SupplierProductRepository quotes = mock(SupplierProductRepository.class);
    final SupplierRepository suppliers = mock(SupplierRepository.class);
    final ProductRepository products = mock(ProductRepository.class);
    final SupplierProductService service = new SupplierProductService(quotes, suppliers, products);
    SupplierProductRequest request(String price) {
        var request = new SupplierProductRequest(); request.setProductId(2L); request.setMinOrderQty(1);
        request.setPurchasePrice(price == null ? null : new BigDecimal(price)); return request;
    }
    SupplierProduct quote(long supplierId, String price) {
        var supplier = new Supplier(); supplier.setId(supplierId); supplier.setSupplierName("Supplier " + supplierId);
        var quote = new SupplierProduct(); quote.setSupplier(supplier); quote.setPurchasePrice(new BigDecimal(price)); return quote;
    }
    @Test void assignmentAndUpdateRetainExactPrices() {
        when(suppliers.findById(1L)).thenReturn(Optional.of(new Supplier()));
        when(products.findById(2L)).thenReturn(Optional.of(new Product()));
        when(quotes.save(any())).thenAnswer(call -> call.getArgument(0));
        var created = service.assignProduct(1L, request("123456789012345.6789"));
        assertEquals(new BigDecimal("123456789012345.6789"), created.getPurchasePrice());
        when(quotes.findBySupplierIdAndProductId(1L, 2L)).thenReturn(Optional.of(created));
        var updated = service.updateProcurementData(1L, 2L, request("123456789012345.6788"));
        assertEquals(new BigDecimal("123456789012345.6788"), updated.getPurchasePrice());
    }
    @Test void almostEqualQuotesSortByExactPriceRatherThanInputOrder() {
        when(products.findById(2L)).thenReturn(Optional.of(new Product()));
        when(quotes.findByProductId(2L)).thenReturn(List.of(
                quote(1, "123456789012345.6789"), quote(2, "123456789012345.6788")));
        var options = service.getSupplierOptionsForProduct(2L);
        assertEquals(2L, options.get(0).getSupplierId());
        assertEquals(new BigDecimal("123456789012345.6788"), options.get(0).getPurchasePrice());
        assertEquals(1L, options.get(1).getSupplierId());
    }
    @Test void invalidQuotePricesFailBeforeAnyRepositoryAccess() {
        for (String price : new String[]{null, "0", "-1", "1000000000000000", "0.00001"}) {
            assertThrows(BusinessRuleException.class, () -> service.assignProduct(1L, request(price)));
            assertThrows(BusinessRuleException.class, () -> service.updateProcurementData(1L, 2L, request(price)));
        }
        verifyNoInteractions(quotes, suppliers, products);
    }
}
