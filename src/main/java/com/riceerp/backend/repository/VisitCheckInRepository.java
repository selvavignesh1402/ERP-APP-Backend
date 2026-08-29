package com.riceerp.backend.repository;

import com.riceerp.backend.entity.VisitCheckIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VisitCheckInRepository extends JpaRepository<VisitCheckIn, Long> {

    Optional<VisitCheckIn> findByVisitScheduleId(Long scheduleId);

    List<VisitCheckIn> findByCustomerIdOrderByCheckInTimeDesc(Long customerId);

    List<VisitCheckIn> findBySalespersonIdAndCheckInTimeBetween(Long salespersonId, LocalDateTime from,
            LocalDateTime to);

    List<VisitCheckIn> findBySalespersonIdOrderByCheckInTimeDesc(Long salespersonId);
}
