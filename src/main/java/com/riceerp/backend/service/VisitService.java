package com.riceerp.backend.service;

import com.riceerp.backend.dto.CheckInRequestDto;
import com.riceerp.backend.dto.CheckOutRequestDto;
import com.riceerp.backend.entity.VisitCheckIn;
import com.riceerp.backend.entity.VisitSchedule;
import com.riceerp.backend.enums.VisitStatus;
import com.riceerp.backend.repository.UserRepository;
import com.riceerp.backend.repository.VisitCheckInRepository;
import com.riceerp.backend.repository.VisitScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class VisitService {

    private final VisitScheduleRepository visitScheduleRepo;
    private final VisitCheckInRepository visitCheckInRepo;
    private final UserRepository userRepo;

    public VisitService(
            VisitScheduleRepository visitScheduleRepo,
            VisitCheckInRepository visitCheckInRepo,
            UserRepository userRepo) {
        this.visitScheduleRepo = visitScheduleRepo;
        this.visitCheckInRepo = visitCheckInRepo;
        this.userRepo = userRepo;
    }

    // ─────────────────────────────────────────────
    // CHECK-IN
    // ─────────────────────────────────────────────

    @Transactional
    public VisitCheckIn checkIn(Long scheduleId, Long salespersonId, CheckInRequestDto dto) {
        VisitSchedule schedule = visitScheduleRepo.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Visit schedule not found: " + scheduleId));

        // Prevent double check-in
        visitCheckInRepo.findByVisitScheduleId(scheduleId).ifPresent(ci -> {
            if (ci.getCheckInTime() != null && ci.getCheckOutTime() == null) {
                throw new RuntimeException("Already checked in for this visit");
            }
        });

        VisitCheckIn checkIn = new VisitCheckIn();
        checkIn.setVisitSchedule(schedule);
        checkIn.setSalesperson(schedule.getSalesperson());
        checkIn.setCustomer(schedule.getCustomer());
        checkIn.setCheckInTime(LocalDateTime.now());
        checkIn.setLatitude(dto.getLatitude());
        checkIn.setLongitude(dto.getLongitude());

        visitCheckInRepo.save(checkIn);
        return checkIn;
    }

    // ─────────────────────────────────────────────
    // CHECK-OUT / COMPLETE VISIT
    // ─────────────────────────────────────────────

    @Transactional
    public VisitCheckIn checkOut(Long checkInId, CheckOutRequestDto dto) {
        VisitCheckIn checkIn = visitCheckInRepo.findById(checkInId)
                .orElseThrow(() -> new RuntimeException("Check-in record not found: " + checkInId));

        checkIn.setCheckOutTime(LocalDateTime.now());
        checkIn.setOutcome(dto.getOutcome());
        checkIn.setNotes(dto.getNotes());
        checkIn.setSaleId(dto.getSaleId());
        checkIn.setPaymentId(dto.getPaymentId());

        visitCheckInRepo.save(checkIn);

        // Mark the schedule as COMPLETED
        VisitSchedule schedule = checkIn.getVisitSchedule();
        schedule.setStatus(VisitStatus.COMPLETED);
        visitScheduleRepo.save(schedule);

        return checkIn;
    }

    // ─────────────────────────────────────────────
    // VISIT HISTORY for a customer
    // ─────────────────────────────────────────────

    public List<VisitCheckIn> getVisitHistory(Long customerId) {
        return visitCheckInRepo.findByCustomerIdOrderByCheckInTimeDesc(customerId);
    }

    // ─────────────────────────────────────────────
    // ALL CHECK-INS for a salesperson
    // ─────────────────────────────────────────────

    public List<VisitCheckIn> getSalespersonHistory(Long salespersonId) {
        return visitCheckInRepo.findBySalespersonIdOrderByCheckInTimeDesc(salespersonId);
    }
}
