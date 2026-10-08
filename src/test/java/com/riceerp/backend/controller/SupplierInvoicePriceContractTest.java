package com.riceerp.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riceerp.backend.dto.SupplierInvoiceItemRequest;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class SupplierInvoicePriceContractTest {
    final ObjectMapper mapper = new ObjectMapper();
    @Test void jsonPriceKeepsEveryDecimalDigit() throws Exception {
        var item = mapper.readValue("{\"productId\":1,\"quantity\":1,\"unitPrice\":123456789012345.6789}", SupplierInvoiceItemRequest.class);
        assertEquals(new BigDecimal("123456789012345.6789"), item.getUnitPrice());
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(item).isEmpty());
            item.setUnitPrice(new BigDecimal("1.00000"));
            assertTrue(factory.getValidator().validate(item).isEmpty());
        }
    }
    @Test void invalidMissingAndNonFinitePricesAreRejected() throws Exception {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            for (String value : new String[]{"null", "0", "-1", "1000000000000000", "1.00001"}) {
                var item = mapper.readValue("{\"productId\":1,\"quantity\":1,\"unitPrice\":" + value + "}", SupplierInvoiceItemRequest.class);
                assertFalse(factory.getValidator().validate(item).isEmpty(), value);
            }
            var missing = mapper.readValue("{\"productId\":1,\"quantity\":1}", SupplierInvoiceItemRequest.class);
            assertFalse(factory.getValidator().validate(missing).isEmpty());
        }
        assertThrows(Exception.class, () -> mapper.readValue("{\"unitPrice\":\"NaN\"}", SupplierInvoiceItemRequest.class));
        assertThrows(Exception.class, () -> mapper.readValue("{\"unitPrice\":\"Infinity\"}", SupplierInvoiceItemRequest.class));
    }
}
