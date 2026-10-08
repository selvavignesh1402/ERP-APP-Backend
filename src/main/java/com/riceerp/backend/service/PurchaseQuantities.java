package com.riceerp.backend.service;

import com.riceerp.backend.entity.PurchaseItem;
import com.riceerp.backend.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Quantity arithmetic only; no conversion between the product's stock units. */
final class PurchaseQuantities {
    private PurchaseQuantities() {}

    static BigDecimal nonNegative(double quantity) {
        if (!Double.isFinite(quantity) || quantity < 0)
            throw new BusinessRuleException("Stock and quantities must be finite and non-negative.");
        return BigDecimal.valueOf(quantity);
    }

    static BigDecimal positive(double quantity) {
        BigDecimal value = nonNegative(quantity);
        if (value.signum() == 0) throw new BusinessRuleException("Quantity must be greater than zero.");
        return value;
    }

    static double stored(BigDecimal quantity) {
        double value = quantity.doubleValue();
        if (!Double.isFinite(value)) throw new BusinessRuleException("Quantity is too large.");
        return value;
    }

    static Map<Long, BigDecimal> ordered(List<PurchaseItem> items) {
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        for (PurchaseItem item : items) {
            if (item.getProduct() == null || item.getProduct().getId() == null)
                throw new BusinessRuleException("Purchase product is missing.");
            result.merge(item.getProduct().getId(), positive(item.getQuantity()), BigDecimal::add);
        }
        if (result.isEmpty()) throw new BusinessRuleException("Purchase has no items.");
        result.values().forEach(PurchaseQuantities::stored);
        return result;
    }
}
