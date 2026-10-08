package com.riceerp.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riceerp.backend.dto.*;
import com.riceerp.backend.entity.Sale;
import com.riceerp.backend.exception.GlobalExceptionHandler;
import com.riceerp.backend.service.SaleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SaleRequestDecimalContractTest {
    private final SaleService service = mock(SaleService.class);
    private final org.springframework.test.web.servlet.MockMvc mvc =
            MockMvcBuilders.standaloneSetup(new SaleController(service))
                    .setControllerAdvice(new GlobalExceptionHandler()).build();

    private String body(String amounts) {
        return "{\"clientReferenceId\":\"sync-decimal\",\"paymentMode\":\"CREDIT\","
                + "\"items\":[{\"productId\":1,\"quantity\":1,\"price\":100}]" + amounts + "}";
    }

    @Test void onlineAndOfflineRequestsPreserveExactDecimals() throws Exception {
        when(service.createSale(any())).thenAnswer(call -> {
            SaleRequest request = call.getArgument(0);
            assertEquals(0, new BigDecimal("123456789012345.67").compareTo(request.getPaidAmount()));
            assertEquals(0, new BigDecimal("99999999999999.1234").compareTo(request.getDiscount()));
            return new Sale();
        });
        when(service.syncBatchSales(any())).thenAnswer(call -> {
            List<OfflineSaleSyncRequest> requests = call.getArgument(0);
            assertEquals(0, new BigDecimal("123456789012345.67").compareTo(requests.get(0).getPaidAmount()));
            assertEquals(0, new BigDecimal("99999999999999.1234").compareTo(requests.get(0).getDiscount()));
            return new SyncBatchResponse();
        });
        String json = body(",\"paidAmount\":123456789012345.6700,\"discount\":99999999999999.1234");
        // clientReferenceId belongs only to the offline request.
        mvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk());
        mvc.perform(post("/api/sales/sync").contentType(MediaType.APPLICATION_JSON).content("[" + json + "]"))
                .andExpect(status().isOk());
        verify(service).createSale(any());
        verify(service).syncBatchSales(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"paidAmount\":-1", "\"paidAmount\":1.001", "\"paidAmount\":1000000000000000",
            "\"discount\":null", "\"discount\":-1", "\"discount\":0.00001", "\"discount\":1000000000000000",
            "\"paidAmount\":\"NaN\"", "\"discount\":\"Infinity\""})
    void invalidOnlineAmountsReturnBadRequestWithoutCallingService(String amount) throws Exception {
        mvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON).content(body("," + amount)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test void omittedDiscountDefaultsToZeroAndPaidAmountRemainsOptional() throws Exception {
        var mapper = new ObjectMapper();
        var online = mapper.readValue("{\"paymentMode\":\"CASH\"}", SaleRequest.class);
        var offline = mapper.readValue("{\"paymentMode\":\"CASH\"}", OfflineSaleSyncRequest.class);
        assertEquals(BigDecimal.ZERO, online.getDiscount());
        assertEquals(BigDecimal.ZERO, offline.getDiscount());
        assertNull(online.getPaidAmount());
        assertNull(offline.getPaidAmount());
    }
}
