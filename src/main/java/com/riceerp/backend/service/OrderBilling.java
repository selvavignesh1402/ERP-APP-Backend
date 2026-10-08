package com.riceerp.backend.service;

import com.riceerp.backend.dto.SaleItemRequest;
import com.riceerp.backend.entity.Sale;
import com.riceerp.backend.entity.SalesOrder;
import com.riceerp.backend.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Reconciles pre-tax order values. Tax treatment remains the existing invoice policy. */
public final class OrderBilling {
    private OrderBilling() {}
    public static BigDecimal money(double amount) {
        if (!Double.isFinite(amount)) throw new BusinessRuleException("Order amounts must be finite.");
        return money(BigDecimal.valueOf(amount));
    }
    public static BigDecimal money(BigDecimal amount) {
        if (amount == null) throw new BusinessRuleException("Order amounts are required.");
        try { return amount.setScale(2, RoundingMode.UNNECESSARY); }
        catch (ArithmeticException ex) { throw new BusinessRuleException("Order amounts must have at most two decimal places."); }
    }
    private static BigDecimal proportion(BigDecimal discount, BigDecimal delivered, BigDecimal subtotal) {
        return discount.multiply(delivered).divide(subtotal, 2, RoundingMode.HALF_UP);
    }
    public static BigDecimal discountFor(SalesOrder order, List<SaleItemRequest> invoice, List<Sale> previous) {
        BigDecimal subtotal = money(order.getSubtotal()), discount = money(order.getDiscount());
        BigDecimal ordered = BigDecimal.ZERO, fulfilled = BigDecimal.ZERO, current = BigDecimal.ZERO;
        for (var line : order.getItems()) {
            BigDecimal price = money(line.getUnitPrice());
            ordered = ordered.add(price.multiply(BigDecimal.valueOf(line.getOrderedQuantity())));
            fulfilled = fulfilled.add(price.multiply(BigDecimal.valueOf(line.getDeliveredQuantity())));
        }
        for (var line : invoice) {
            var orderedLine = order.getItems().stream().filter(item -> item.getProduct().getId().equals(line.getProductId()))
                    .findFirst().orElseThrow(() -> new BusinessRuleException("Invoice product is not in the order."));
            if (money(line.getPrice()).compareTo(money(orderedLine.getUnitPrice())) != 0)
                throw new BusinessRuleException("Dispatch price differs from the agreed order price.");
            current = current.add(money(line.getPrice()).multiply(BigDecimal.valueOf(line.getQuantity())));
        }
        BigDecimal billed = BigDecimal.ZERO, allocated = BigDecimal.ZERO;
        for (Sale sale : previous) {
            billed = billed.add(money(sale.getTotal())); allocated = allocated.add(money(sale.getDiscount()));
        }
        if (subtotal.signum() <= 0 || subtotal.compareTo(ordered) != 0 || discount.signum() < 0 || discount.compareTo(subtotal) > 0 ||
                billed.signum() < 0 || billed.add(current).compareTo(fulfilled) != 0 || fulfilled.compareTo(subtotal) > 0 ||
                allocated.compareTo(proportion(discount, billed, subtotal)) != 0)
            throw new BusinessRuleException("Order invoices do not match its fulfilled quantities or discount. Reconcile earlier invoices before continuing.");
        // Difference of cumulative rounded targets allocates the final cent once.
        BigDecimal share = proportion(discount, fulfilled, subtotal).subtract(allocated);
        if (share.signum() < 0 || share.compareTo(current) > 0)
            throw new BusinessRuleException("Invalid remaining order discount allocation.");
        return share;
    }
}
