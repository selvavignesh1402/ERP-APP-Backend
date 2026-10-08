package com.riceerp.backend.service;

import com.riceerp.backend.enums.TaxType;
import com.riceerp.backend.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class GstCalculatorTest {
    private GstCalculator.Line line(String amount, Double rate) { return new GstCalculator.Line(new BigDecimal(amount), rate); }
    @Test void noGstChargesNothingForEitherSupplyType() {
        for (var type : TaxType.values()) {
            var result = GstCalculator.calculate(List.of(line("1000", null)), new BigDecimal("100"), type);
            assertEquals(0, result.total().signum());
        }
    }
    @Test void mixedRatesDiscountAndExemptGoodsUseProportionalTaxableValues() {
        var lines = List.of(line("1000", null), line("1000", 5.0), line("1000", 18.0));
        var local = GstCalculator.calculate(lines, new BigDecimal("300"), TaxType.INTRA_STATE);
        assertEquals(new BigDecimal("103.50"), local.cgst());
        assertEquals(local.cgst(), local.sgst());
        assertEquals(0, local.igst().signum());
        var interstate = GstCalculator.calculate(lines, new BigDecimal("300"), TaxType.INTER_STATE);
        assertEquals(new BigDecimal("207.00"), interstate.igst());
        assertEquals(0, interstate.cgst().signum());
        assertEquals(0, interstate.sgst().signum());
    }
    @Test void splittingSameRateIntoLinesDoesNotChangeRounding() {
        var first = GstCalculator.calculate(List.of(line("0.3", 5.0)), BigDecimal.ZERO, TaxType.INTRA_STATE);
        var split = GstCalculator.calculate(List.of(line("0.1", 5.0), line("0.2", 5.0)), BigDecimal.ZERO, TaxType.INTRA_STATE);
        assertEquals(new BigDecimal("0.02"), first.total());
        assertEquals(first, split);
    }
    @Test void fullDiscountAndZeroValueProduceNoTax() {
        assertEquals(0, GstCalculator.calculate(List.of(line("100", 18.0)), new BigDecimal("100"), TaxType.INTER_STATE).total().signum());
        assertEquals(0, GstCalculator.calculate(List.of(line("0", null)), BigDecimal.ZERO, TaxType.INTRA_STATE).total().signum());
    }
    @ParameterizedTest @ValueSource(doubles = {-1, 7, 101, Double.NaN, Double.POSITIVE_INFINITY})
    void invalidRatesAreRejected(double rate) { assertThrows(BusinessRuleException.class, () -> GstCalculator.rate(rate)); }
    @ParameterizedTest @ValueSource(doubles = {0.25, 1.5, 3, 5, 18, 40})
    void supportedRatesAreKept(double rate) { assertEquals(rate, GstCalculator.rate(rate)); }
    @Test void zeroRateNormalizesToEmptyAndSupplyTypeIsRequired() {
        assertNull(GstCalculator.rate(0.0));
        assertThrows(BusinessRuleException.class, () -> GstCalculator.calculate(List.of(line("100", 5.0)), BigDecimal.ZERO, null));
    }
}
