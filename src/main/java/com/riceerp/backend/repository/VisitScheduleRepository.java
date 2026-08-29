package com.riceerp.backend.repository;

import com.riceerp.backend.entity.VisitSchedule;
import com.riceerp.backend.enums.VisitStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VisitScheduleRepository extends JpaRepository<VisitSchedule, Long> {

    List<VisitSchedule> findBySalespersonIdAndScheduledDateOrderByVisitOrderAsc(Long salespersonId, LocalDate date);

    List<VisitSchedule> findByScheduledDateBetween(LocalDate from, LocalDate to);

    List<VisitSchedule> findBySalespersonIdAndScheduledDateBetween(Long salespersonId, LocalDate from, LocalDate to);

    boolean existsByBeatPlanIdAndScheduledDate(Long beatPlanId, LocalDate date);

    @Query("SELECT vs FROM VisitSchedule vs WHERE vs.scheduledDate = :date ORDER BY vs.salesperson.name, vs.visitOrder")
    List<VisitSchedule> findAllForDate(@Param("date") LocalDate date);

    long countBySalespersonIdAndScheduledDateAndStatus(Long salespersonId, LocalDate date, VisitStatus status);
}
