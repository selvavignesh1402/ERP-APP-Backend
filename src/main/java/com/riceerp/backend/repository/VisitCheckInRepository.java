package com.riceerp.backend.repository;

import com.riceerp.backend.entity.VisitCheckIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VisitCheckInRepository extends JpaRepository<VisitCheckIn, Long> {
    @org.springframework.data.jpa.repository.Query("SELECT v.visitSchedule.id FROM VisitCheckIn v WHERE v.id = :id AND v.organizationId = :orgId")
    Optional<Long> findScheduleIdForCheckOut(
            @org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("orgId") Long orgId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT v FROM VisitCheckIn v WHERE v.visitSchedule.id = :scheduleId AND v.organizationId = :orgId")
    Optional<VisitCheckIn> findForUpdateBySchedule(
            @org.springframework.data.repository.query.Param("scheduleId") Long scheduleId,
            @org.springframework.data.repository.query.Param("orgId") Long orgId);

    Optional<VisitCheckIn> findByVisitScheduleId(Long scheduleId);

    List<VisitCheckIn> findByCustomerIdOrderByCheckInTimeDesc(Long customerId);

    List<VisitCheckIn> findBySalespersonIdAndCheckInTimeBetween(Long salespersonId, LocalDateTime from,
            LocalDateTime to);

    List<VisitCheckIn> findBySalespersonIdOrderByCheckInTimeDesc(Long salespersonId);
}
