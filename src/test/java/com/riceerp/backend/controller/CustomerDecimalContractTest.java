package com.riceerp.backend.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.riceerp.backend.dto.CustomerRequest;
import com.riceerp.backend.dto.CustomerResponse;
import com.riceerp.backend.dto.TodayRouteDto;
import com.riceerp.backend.entity.Customer;
import com.riceerp.backend.service.CustomerService;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomerDecimalContractTest {
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

    @Test void requestAndCustomerRouteAndBalanceResponsesKeepExactNumbers() throws Exception {
        var limit = new BigDecimal("123456789012345.6789");
        var request = mapper.readValue("{\"creditLimit\":123456789012345.6789}", CustomerRequest.class);
        assertEquals(limit, request.getCreditLimit());
        var customer = new Customer();
        customer.setCreditLimit(request.getCreditLimit());
        customer.setCreditBalance(new BigDecimal("123456789012345.6788"));
        var response = mapper.readTree(mapper.writeValueAsString(CustomerResponse.from(customer)));
        assertTrue(response.get("creditLimit").isNumber());
        assertEquals(0, limit.compareTo(response.get("creditLimit").decimalValue()));
        assertEquals(0, customer.getCreditBalance().compareTo(response.get("creditBalance").decimalValue()));
        var route = new TodayRouteDto();
        route.setCreditLimit(customer.getCreditLimit());
        route.setOutstandingBalance(customer.getCreditBalance());
        var routeJson = mapper.readTree(mapper.writeValueAsString(route));
        assertEquals(0, limit.compareTo(routeJson.get("creditLimit").decimalValue()));
        assertEquals(0, customer.getCreditBalance().compareTo(routeJson.get("outstandingBalance").decimalValue()));
        var service = mock(CustomerService.class);
        when(service.getCustomerById(1L)).thenReturn(customer);
        assertEquals(new BigDecimal("0.0001"), new CustomerController(service).getBalance(1L).get("available"));
    }

    @Test void omittedLimitDefaultsToZeroAndInvalidExplicitLimitsFailValidation() throws Exception {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var request = mapper.readValue("{\"customerName\":\"Shop\",\"phone\":\"9999999999\"}", CustomerRequest.class);
            assertEquals(BigDecimal.ZERO, request.getCreditLimit());
            assertTrue(validator.validate(request).isEmpty());
            for (var value : new String[] {"null", "-0.01", "1000000000000000", "0.00001"}) {
                request = mapper.readValue("{\"customerName\":\"Shop\",\"phone\":\"9999999999\",\"creditLimit\":" + value + "}", CustomerRequest.class);
                assertFalse(validator.validate(request).isEmpty(), value);
            }
        }
    }
}
