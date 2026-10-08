package com.riceerp.backend.controller;
import com.riceerp.backend.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SafeErrorsTest {
    final GlobalExceptionHandler errors = new GlobalExceptionHandler();
    @Test void unexpectedErrorsDoNotExposeDatabaseOrInternalDetails() {
        var response = errors.handleRuntimeException(new RuntimeException("SQL password=private internal-table-name"));
        assertEquals(500, response.getStatusCode().value()); assertEquals("INTERNAL_SERVER_ERROR", response.getBody().get("code"));
        assertFalse(response.getBody().toString().contains("private"));
    }
    @Test void duplicateDatabaseRecordsAreConflictsWithoutSql() {
        var response = errors.handleIntegrity(new org.springframework.dao.DataIntegrityViolationException("SQL private-table"));
        assertEquals(409, response.getStatusCode().value()); assertFalse(response.getBody().toString().contains("private-table"));
    }
    @Test void invalidCredentialsAreUnauthorizedRatherThanServerErrors() {
        assertEquals(401, errors.handleAuthentication(new org.springframework.security.authentication.BadCredentialsException("detail")).getStatusCode().value());
    }
}
