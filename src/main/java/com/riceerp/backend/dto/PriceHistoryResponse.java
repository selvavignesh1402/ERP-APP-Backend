package com.riceerp.backend.dto;
import com.riceerp.backend.entity.PriceHistory;
import com.riceerp.backend.enums.PriceType;
import java.time.LocalDateTime;

public record PriceHistoryResponse(Long id, ProductResponse product, PriceType priceType,
        java.math.BigDecimal price, LocalDateTime effectiveFrom, Long organizationId) {
    public static PriceHistoryResponse from(PriceHistory e) {
        return new PriceHistoryResponse(e.getId(), ProductResponse.from(e.getProduct()), e.getPriceType(),
                e.getPrice(), e.getEffectiveFrom(), e.getOrganizationId());
    }
}
