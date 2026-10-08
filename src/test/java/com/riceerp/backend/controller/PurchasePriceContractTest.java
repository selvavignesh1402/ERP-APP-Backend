package com.riceerp.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riceerp.backend.dto.*;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class PurchasePriceContractTest {
    final ObjectMapper mapper = new ObjectMapper();

    @Test void purchaseAndReceiptPricesRetainExactJsonDecimals() throws Exception {
        var purchase = mapper.readValue("{\"productId\":1,\"quantity\":1,\"price\":123456789012345.6789}", PurchaseItemRequest.class);
        var receipt = mapper.readValue("{\"productId\":1,\"receivedQty\":1,\"unitPrice\":123456789012345.6789}", GoodsReceiptItemRequest.class);
        assertEquals(new BigDecimal("123456789012345.6789"), purchase.getPrice());
        assertEquals(purchase.getPrice(), receipt.getUnitPrice());
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(purchase).isEmpty());
            assertTrue(factory.getValidator().validate(receipt).isEmpty());
        }
    }

    @Test void invalidAndMissingPricesAreRejected() throws Exception {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            for (String price : new String[]{"null", "0", "-1", "1000000000000000", "1.00001"}) {
                var purchase = mapper.readValue("{\"productId\":1,\"quantity\":1,\"price\":" + price + "}", PurchaseItemRequest.class);
                var receipt = mapper.readValue("{\"productId\":1,\"receivedQty\":1,\"unitPrice\":" + price + "}", GoodsReceiptItemRequest.class);
                assertFalse(factory.getValidator().validate(purchase).isEmpty(), price);
                assertFalse(factory.getValidator().validate(receipt).isEmpty(), price);
            }
            assertFalse(factory.getValidator().validate(mapper.readValue("{\"productId\":1,\"quantity\":1}", PurchaseItemRequest.class)).isEmpty());
            assertFalse(factory.getValidator().validate(mapper.readValue("{\"productId\":1,\"receivedQty\":1}", GoodsReceiptItemRequest.class)).isEmpty());
        }
        assertThrows(Exception.class, () -> mapper.readValue("{\"price\":\"NaN\"}", PurchaseItemRequest.class));
        assertThrows(Exception.class, () -> mapper.readValue("{\"unitPrice\":\"Infinity\"}", GoodsReceiptItemRequest.class));
    }
}
