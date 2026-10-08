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
    List<VisitSchedule> findByBeatPlanIdAndScheduledDateGreaterThanEqual(Long beatPlanId, LocalDate date);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true)
    @Query("update VisitSchedule v set v.status = com.riceerp.backend.enums.VisitStatus.MISSED " +
            "where v.organizationId=:orgId and v.scheduledDate < :today and v.status=com.riceerp.backend.enums.VisitStatus.PENDING " +
            "and not exists (select c.id from VisitCheckIn c where c.visitSchedule.id=v.id)")
    int expireUnstartedSchedules(@Param("orgId") Long orgId, @Param("today") LocalDate today);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM VisitSchedule v WHERE v.id = :id AND v.organizationId = :orgId")
    java.util.Optional<VisitSchedule> findForUpdate(@Param("id") Long id, @Param("orgId") Long orgId);

    List<VisitSchedule> findBySalespersonIdAndScheduledDateOrderByVisitOrderAsc(Long salespersonId, LocalDate date);

    List<VisitSchedule> findByScheduledDateBetween(LocalDate from, LocalDate to);

    List<VisitSchedule> findBySalespersonIdAndScheduledDateBetween(Long salespersonId, LocalDate from, LocalDate to);

    boolean existsByBeatPlanIdAndScheduledDate(Long beatPlanId, LocalDate date);

    boolean existsByBeatPlanIdAndCustomerIdAndScheduledDate(Long beatPlanId, Long customerId, LocalDate date);

    @Query("SELECT vs FROM VisitSchedule vs WHERE vs.scheduledDate = :date ORDER BY vs.salesperson.name, vs.visitOrder")
    List<VisitSchedule> findAllForDate(@Param("date") LocalDate date);

    long countBySalespersonIdAndScheduledDateAndStatus(Long salespersonId, LocalDate date, VisitStatus status);
}
