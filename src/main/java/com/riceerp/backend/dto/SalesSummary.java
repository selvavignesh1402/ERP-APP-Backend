package com.riceerp.backend.dto;

import com.riceerp.backend.entity.Sale;
import com.riceerp.backend.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** Invoice values include saved discounts/taxes. Payments applied are not cash receipts by payment date. */
public record SalesSummary(long invoiceCount, BigDecimal invoicedTotal, BigDecimal paymentsApplied,
                           BigDecimal outstanding, LocalDateTime asOf, LocalDate periodStart, LocalDate periodEnd,
                           List<DailySales> trend) {
    public record DailySales(LocalDate date, BigDecimal amount) {}
    private static BigDecimal money(BigDecimal amount) {
        if (amount == null || amount.signum() < 0) throw new BusinessRuleException("Invalid saved invoice amount; report cannot be calculated.");
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
    public static SalesSummary fromSales(List<Sale> sales, LocalDateTime now) {
        LocalDate end = now.toLocalDate(), start = end.minusDays(6);
        Map<LocalDate, BigDecimal> daily = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) daily.put(start.plusDays(i), BigDecimal.ZERO.setScale(2));
        BigDecimal total = BigDecimal.ZERO, paid = BigDecimal.ZERO, due = BigDecimal.ZERO;
        for (Sale sale : sales) {
            BigDecimal amount = money(sale.getGrandTotal());
            if (sale.getPaidAmount() == null || sale.getBalanceDue() == null)
                throw new BusinessRuleException("Invoice payment totals are unavailable.");
            total = total.add(amount); paid = paid.add(money(sale.getPaidAmount())); due = due.add(money(sale.getBalanceDue()));
            if (sale.getSaleDate() != null && daily.containsKey(sale.getSaleDate().toLocalDate()))
                daily.merge(sale.getSaleDate().toLocalDate(), amount, BigDecimal::add);
        }
        return new SalesSummary(sales.size(), total, paid, due, now, start, end,
                daily.entrySet().stream().map(entry -> new DailySales(entry.getKey(), entry.getValue())).toList());
    }
}
