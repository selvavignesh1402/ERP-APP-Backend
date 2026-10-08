package com.riceerp.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riceerp.backend.dto.SalesOrderRequest;
import com.riceerp.backend.entity.SalesOrder;
import com.riceerp.backend.exception.GlobalExceptionHandler;
import com.riceerp.backend.service.SalesOrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SalesOrderDiscountContractTest {
    final SalesOrderService service = mock(SalesOrderService.class);
    final org.springframework.test.web.servlet.MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new SalesOrderController(service))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
    String body(String discount) {
        return "{\"customerId\":1,\"items\":[{\"productId\":1,\"quantity\":1,\"unitPrice\":100}],\"discount\":" + discount + "}";
    }
    @Test void discountDeserializesExactlyWithoutFloatingPointConversion() throws Exception {
        when(service.createSalesOrder(any())).thenAnswer(call -> {
            SalesOrderRequest request = call.getArgument(0);
            assertEquals(0, new BigDecimal("999999999999999.99").compareTo(request.getDiscount()));
            return new SalesOrder();
        });
        mvc.perform(post("/api/sales-orders").contentType(MediaType.APPLICATION_JSON)
                .content(body("999999999999999.9900"))).andExpect(status().isOk());
        verify(service).createSalesOrder(any());
    }
    @ParameterizedTest
    @ValueSource(strings = {"null", "-0.01", "0.001", "1000000000000000", "\"NaN\"", "\"Infinity\""})
    void invalidDiscountDoesNotReachTheService(String discount) throws Exception {
        mvc.perform(post("/api/sales-orders").contentType(MediaType.APPLICATION_JSON)
                .content(body(discount))).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void omittedDiscountRemainsZero() throws Exception {
        assertEquals(BigDecimal.ZERO, new ObjectMapper().readValue("{}", SalesOrderRequest.class).getDiscount());
    }
}
