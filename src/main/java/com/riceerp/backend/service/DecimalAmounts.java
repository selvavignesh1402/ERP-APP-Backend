package com.riceerp.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import com.riceerp.backend.exception.BusinessRuleException;

/** Decimal arithmetic at calculation boundaries; existing API numbers remain compatible. */
public final class DecimalAmounts {
    private DecimalAmounts() { }
    public static BigDecimal value(double amount) {
        if (!Double.isFinite(amount)) throw new BusinessRuleException("Amounts must be finite.");
        return BigDecimal.valueOf(amount);
    }
    public static BigDecimal cents(BigDecimal amount) { return amount.setScale(2, RoundingMode.HALF_UP); }
    public static double cents(double amount) { return cents(value(amount)).doubleValue(); }
}
