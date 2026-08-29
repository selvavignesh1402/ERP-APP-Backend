package com.riceerp.backend.repository;

import com.riceerp.backend.entity.BeatPlanEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;

@Repository
public interface BeatPlanEntryRepository extends JpaRepository<BeatPlanEntry, Long> {
    List<BeatPlanEntry> findByBeatPlanId(Long beatPlanId);

    List<BeatPlanEntry> findByBeatPlanIdAndDayOfWeek(Long beatPlanId, DayOfWeek dayOfWeek);

    void deleteByBeatPlanId(Long beatPlanId);
}
