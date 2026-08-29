package com.riceerp.backend.repository;

import com.riceerp.backend.entity.StockAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, Long> {
    List<StockAdjustment> findByProductIdOrderByAdjustedAtDesc(Long productId);
}
