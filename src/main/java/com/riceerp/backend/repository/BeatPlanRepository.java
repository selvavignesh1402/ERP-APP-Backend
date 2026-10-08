package com.riceerp.backend.repository;

import com.riceerp.backend.entity.BeatPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BeatPlanRepository extends JpaRepository<BeatPlan, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select o from Organization o where o.id=:id")
    java.util.Optional<com.riceerp.backend.entity.Organization> lockOrganization(@org.springframework.data.repository.query.Param("id") Long id);
    List<BeatPlan> findByIsActiveTrue();

    List<BeatPlan> findBySalespersonId(Long salespersonId);

    List<BeatPlan> findBySalespersonIdAndIsActiveTrue(Long salespersonId);
}
