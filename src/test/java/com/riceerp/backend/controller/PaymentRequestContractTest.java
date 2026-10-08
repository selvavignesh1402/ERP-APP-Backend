package com.riceerp.backend.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riceerp.backend.dto.PaymentRequest;
import com.riceerp.backend.entity.Payment;
import com.riceerp.backend.exception.GlobalExceptionHandler;
import com.riceerp.backend.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PaymentRequestContractTest {
    private final PaymentService service = mock(PaymentService.class);
    private final org.springframework.test.web.servlet.MockMvc mvc =
            MockMvcBuilders.standaloneSetup(new PaymentController(service))
                    .setControllerAdvice(new GlobalExceptionHandler()).build();

    private String body(String amount) {
        return "{\"referenceType\":\"CUSTOMER\",\"referenceId\":1,\"paymentMode\":\"CASH\","
                + "\"clientReferenceId\":\"decimal-request\"" + (amount == null ? "" : ",\"amount\":" + amount) + "}";
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.01", "10.0000", "1e2", "123456789012345.67", "999999999999999.99"})
    void numericRequestReachesServiceExactlyAndResponseRemainsNumeric(String amount) throws Exception {
        when(service.createPayment(any())).thenAnswer(invocation -> {
            PaymentRequest request = invocation.getArgument(0);
            var payment = new Payment();
            payment.setAmount(request.getAmount());
            return payment;
        });
        var result = mvc.perform(post("/api/payments").contentType(MediaType.APPLICATION_JSON).content(body(amount)))
                .andExpect(status().isOk()).andReturn();
        var captured = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(service).createPayment(captured.capture());
        var expected = new BigDecimal(amount);
        assertEquals(0, expected.compareTo(captured.getValue().getAmount()));
        var json = new ObjectMapper().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .readTree(result.getResponse().getContentAsString());
        assertTrue(json.get("amount").isNumber());
        assertEquals(0, expected.compareTo(json.get("amount").decimalValue()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "0", "-0.01", "1.001", "1000000000000000", "1e30", "1e-30",
            "\"NaN\"", "\"Infinity\"", "NaN", "Infinity", "\"not-a-number\"", "true", "{}"})
    void invalidAmountsReturnBadRequestWithoutCallingService(String amount) throws Exception {
        mvc.perform(post("/api/payments").contentType(MediaType.APPLICATION_JSON).content(body(amount)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test void omittedAmountReturnsRequiredFieldError() throws Exception {
        mvc.perform(post("/api/payments").contentType(MediaType.APPLICATION_JSON).content(body(null)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.amount").value("Amount is required"));
        verifyNoInteractions(service);
    }
}
