package com.riceerp.backend.controller;

import com.riceerp.backend.dto.SupplierProductRequest;
import com.riceerp.backend.entity.SupplierProduct;
import com.riceerp.backend.service.SupplierProductService;
import com.riceerp.backend.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SupplierQuoteContractTest {
    final SupplierProductService service = mock(SupplierProductService.class);
    final org.springframework.test.web.servlet.MockMvc mvc = MockMvcBuilders.standaloneSetup(new SupplierProductController(service))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
    String body(String price) { return "{\"productId\":2,\"minOrderQty\":1" + price + "}"; }
    @Test void priceIsExactOnInputAndRemainsNumericOnOutput() throws Exception {
        var price = new BigDecimal("123456789012345.6789");
        when(service.assignProduct(eq(1L), any())).thenAnswer(call -> {
            SupplierProductRequest request = call.getArgument(1); assertEquals(price, request.getPurchasePrice());
            var quote = new SupplierProduct(); quote.setPurchasePrice(request.getPurchasePrice()); return quote;
        });
        var result = mvc.perform(post("/api/suppliers/1/products").contentType(MediaType.APPLICATION_JSON)
                .content(body(",\"purchasePrice\":123456789012345.678900"))).andExpect(status().isOk()).andReturn();
        var json = new ObjectMapper().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .readTree(result.getResponse().getContentAsString());
        assertTrue(json.get("purchasePrice").isNumber()); assertEquals(price, json.get("purchasePrice").decimalValue());
    }
    @Test void invalidCreateAndUpdatePricesNeverReachTheService() throws Exception {
        for (String field : new String[]{"", ",\"purchasePrice\":null", ",\"purchasePrice\":0", ",\"purchasePrice\":-1",
                ",\"purchasePrice\":1.00001", ",\"purchasePrice\":1000000000000000", ",\"purchasePrice\":\"NaN\"", ",\"purchasePrice\":\"Infinity\""}) {
            mvc.perform(post("/api/suppliers/1/products").contentType(MediaType.APPLICATION_JSON).content(body(field))).andExpect(status().isBadRequest());
            mvc.perform(put("/api/suppliers/1/products/2").contentType(MediaType.APPLICATION_JSON).content(body(field))).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }
}
