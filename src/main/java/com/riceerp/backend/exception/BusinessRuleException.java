package com.riceerp.backend.exception;

/**
 * Thrown when a request violates a business rule (insufficient stock,
 * credit limit exceeded, invalid status transition, duplicate record).
 * Maps to HTTP 409 Conflict.
 */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
