package com.riceerp.backend.service;

import com.riceerp.backend.enums.TaxType;
import com.riceerp.backend.exception.BusinessRuleException;
import java.math.*;
import java.util.*;

/** Tax-exclusive prices; invoice discount is apportioned across GST-rate buckets. */
public final class GstCalculator {
    private GstCalculator() {}
    public static final Set<Double> RATES = Set.of(0.25, 1.5, 3.0, 5.0, 18.0, 40.0);
    public record Line(BigDecimal amount, Double rate) {}
    public record Totals(BigDecimal cgst, BigDecimal sgst, BigDecimal igst) {
        public BigDecimal total() { return cgst.add(sgst).add(igst); }
    }
    public static Double rate(Double value) {
        if (value == null || value == 0) return null;
        if (!RATES.contains(value)) throw new BusinessRuleException("Select a supported GST rate or No GST.");
        return value;
    }
    public static Totals calculate(List<Line> lines, BigDecimal discount, TaxType type) {
        if (type == null) throw new BusinessRuleException("Select intra-state or interstate supply.");
        var buckets = new TreeMap<BigDecimal, BigDecimal>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Line line : lines) {
            Double normalized = rate(line.rate());
            BigDecimal gst = normalized == null ? BigDecimal.ZERO : BigDecimal.valueOf(normalized);
            if (line.amount().signum() < 0) throw new BusinessRuleException("Taxable values cannot be negative.");
            subtotal = subtotal.add(line.amount());
            buckets.merge(gst, line.amount(), BigDecimal::add);
        }
        if (discount == null || discount.signum() < 0 || discount.compareTo(subtotal) > 0)
            throw new BusinessRuleException("Discount must be between zero and the subtotal.");
        BigDecimal component = BigDecimal.ZERO.setScale(2);
        if (subtotal.signum() > 0) {
            BigDecimal net = subtotal.subtract(discount);
            for (var bucket : buckets.entrySet()) {
                BigDecimal taxable = bucket.getValue().multiply(net).divide(subtotal, MathContext.DECIMAL128);
                BigDecimal divisor = type == TaxType.INTER_STATE ? new BigDecimal("100") : new BigDecimal("200");
                component = component.add(taxable.multiply(bucket.getKey()).divide(divisor).setScale(2, RoundingMode.HALF_UP));
            }
        }
        return type == TaxType.INTER_STATE
                ? new Totals(BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2), component)
                : new Totals(component, component, BigDecimal.ZERO.setScale(2));
    }
}
