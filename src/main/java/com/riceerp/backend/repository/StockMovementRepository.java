package com.riceerp.backend.repository;

import com.riceerp.backend.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findByOrganizationIdAndIdLessThanOrderByIdDesc(Long organizationId, Long beforeId,
                                                                    org.springframework.data.domain.Pageable pageable);
    List<StockMovement> findByProductId(Long productId);
    List<StockMovement> findByProductIdAndCreatedAtBetween(Long productId, LocalDateTime start, LocalDateTime end);
    List<StockMovement> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
