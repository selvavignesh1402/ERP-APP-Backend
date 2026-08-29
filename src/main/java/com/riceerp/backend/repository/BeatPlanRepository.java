package com.riceerp.backend.repository;

import com.riceerp.backend.entity.BeatPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BeatPlanRepository extends JpaRepository<BeatPlan, Long> {
    List<BeatPlan> findByIsActiveTrue();

    List<BeatPlan> findBySalespersonId(Long salespersonId);

    List<BeatPlan> findBySalespersonIdAndIsActiveTrue(Long salespersonId);
}
