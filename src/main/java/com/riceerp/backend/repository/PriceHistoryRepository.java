package com.riceerp.backend.repository;

import com.riceerp.backend.entity.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {
    List<PriceHistory> findByProductIdOrderByEffectiveFromDesc(Long productId);
}
