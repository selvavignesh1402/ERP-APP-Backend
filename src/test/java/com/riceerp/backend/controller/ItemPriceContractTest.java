package com.riceerp.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riceerp.backend.dto.*;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class ItemPriceContractTest {
    final ObjectMapper mapper = new ObjectMapper();
    @Test void saleAndOrderPricesDeserializeWithoutDoubleRounding() throws Exception {
        var sale = mapper.readValue("{\"productId\":1,\"quantity\":1,\"price\":123456789012345.6789}", SaleItemRequest.class);
        var order = mapper.readValue("{\"productId\":1,\"quantity\":1,\"unitPrice\":123456789012345.6700}", SalesOrderItemRequest.class);
        assertEquals(new BigDecimal("123456789012345.6789"), sale.getPrice());
        assertEquals(new BigDecimal("123456789012345.67"), order.getUnitPrice());
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(sale).isEmpty());
            assertTrue(factory.getValidator().validate(order).isEmpty());
        }
    }
    @Test void invalidPricePrecisionRangeAndNullAreRejected() throws Exception {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            for (String value : new String[]{"null", "-0.01", "1000000000000000", "1.00001"}) {
                var sale = mapper.readValue("{\"productId\":1,\"quantity\":1,\"price\":" + value + "}", SaleItemRequest.class);
                assertFalse(factory.getValidator().validate(sale).isEmpty(), value);
            }
            for (String value : new String[]{"null", "-0.01", "1000000000000000", "1.001"}) {
                var order = mapper.readValue("{\"productId\":1,\"quantity\":1,\"unitPrice\":" + value + "}", SalesOrderItemRequest.class);
                assertFalse(factory.getValidator().validate(order).isEmpty(), value);
            }
        }
        assertThrows(Exception.class, () -> mapper.readValue("{\"price\":\"NaN\"}", SaleItemRequest.class));
        assertThrows(Exception.class, () -> mapper.readValue("{\"unitPrice\":\"Infinity\"}", SalesOrderItemRequest.class));
    }
}
