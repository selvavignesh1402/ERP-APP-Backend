package com.riceerp.backend.controller;

import com.riceerp.backend.entity.Product;
import com.riceerp.backend.service.ProductService;
import com.riceerp.backend.service.SupplierProductService;
import com.riceerp.backend.service.StockAdjustmentService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CanonicalRoutesTest {
    @Test void productAliasesPreserveBusinessResponseWithoutEntityVersion() throws Exception {
        ProductService products = mock(ProductService.class);
        Product product = new Product(); product.setId(9L); product.setProductName("Rice");
        product.setStock(10); product.setReservedStock(3.0);
        when(products.listProducts(null, null)).thenReturn(List.of(product));
        var mvc = MockMvcBuilders.standaloneSetup(new ProductController(products, mock(SupplierProductService.class))).build();
        for (String route : List.of("/products", "/api/products")) {
            mvc.perform(get(route)).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].productName").value("Rice"))
                    .andExpect(jsonPath("$[0].availableStock").value(7.0))
                    .andExpect(jsonPath("$[0].version").doesNotExist());
        }
    }

    @Test void adjustmentResourceAndLegacyInventoryPathRemainReachable() throws Exception {
        StockAdjustmentService adjustments = mock(StockAdjustmentService.class);
        when(adjustments.listAdjustmentsByProduct(9L)).thenReturn(List.of());
        var mvc = MockMvcBuilders.standaloneSetup(new StockAdjustmentController(adjustments)).build();
        for (String route : List.of("/inventory/adjustments", "/api/stock-adjustments")) {
            mvc.perform(get(route).param("productId", "9")).andExpect(status().isOk()).andExpect(content().json("[]"));
        }
        verify(adjustments, times(2)).listAdjustmentsByProduct(9L);
    }
}
