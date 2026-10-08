package com.riceerp.backend.service;

import com.riceerp.backend.dto.CheckInRequestDto;
import com.riceerp.backend.dto.CheckOutRequestDto;
import com.riceerp.backend.entity.VisitCheckIn;
import com.riceerp.backend.entity.VisitSchedule;
import com.riceerp.backend.enums.VisitStatus;
import com.riceerp.backend.repository.SaleRepository;
import com.riceerp.backend.repository.PaymentRepository;
import com.riceerp.backend.entity.Customer;
import com.riceerp.backend.entity.User;
import com.riceerp.backend.entity.Sale;
import com.riceerp.backend.entity.Payment;
import com.riceerp.backend.enums.ReferenceType;
import com.riceerp.backend.exception.BusinessRuleException;
import com.riceerp.backend.exception.NotFoundException;
import com.riceerp.backend.security.TenantContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.Objects;
import com.riceerp.backend.repository.VisitCheckInRepository;
import com.riceerp.backend.repository.VisitScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class VisitService {

    private final VisitScheduleRepository visitScheduleRepo;
    private final VisitCheckInRepository visitCheckInRepo;
    private final SaleRepository saleRepo;
    private final PaymentRepository paymentRepo;

    public VisitService(
            VisitScheduleRepository visitScheduleRepo,
            VisitCheckInRepository visitCheckInRepo,
            SaleRepository saleRepo,
            PaymentRepository paymentRepo) {
        this.visitScheduleRepo = visitScheduleRepo;
        this.visitCheckInRepo = visitCheckInRepo;
        this.saleRepo = saleRepo;
        this.paymentRepo = paymentRepo;
    }

    // ─────────────────────────────────────────────
    // CHECK-IN
    // ─────────────────────────────────────────────

    @Transactional
    @PreAuthorize("hasAuthority('visit:execute')")
    public VisitCheckIn checkIn(Long scheduleId, Long salespersonId, CheckInRequestDto dto) {
        Long orgId = currentOrganization();
        VisitSchedule schedule = visitScheduleRepo.findForUpdate(scheduleId, orgId)
                .orElseThrow(() -> new NotFoundException("Visit schedule not found"));
        requireOwner(schedule.getSalesperson(), salespersonId);
        requirePending(schedule);
        requireScheduledToday(schedule);

        // All visit mutations lock the schedule first, serializing repeated requests.
        visitCheckInRepo.findForUpdateBySchedule(scheduleId, orgId).ifPresent(ci -> {
            throw new BusinessRuleException("This visit already has a check-in");
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
    @PreAuthorize("hasAuthority('visit:execute')")
    public VisitCheckIn checkOut(Long checkInId, Long salespersonId, CheckOutRequestDto dto) {
        Long orgId = currentOrganization();
        Long scheduleId = visitCheckInRepo.findScheduleIdForCheckOut(checkInId, orgId)
                .orElseThrow(() -> new NotFoundException("Check-in record not found"));
        VisitSchedule schedule = visitScheduleRepo.findForUpdate(scheduleId, orgId)
                .orElseThrow(() -> new NotFoundException("Visit schedule not found"));
        requireOwner(schedule.getSalesperson(), salespersonId);
        VisitCheckIn checkIn = visitCheckInRepo.findForUpdateBySchedule(schedule.getId(), orgId)
                .filter(record -> Objects.equals(record.getId(), checkInId))
                .orElseThrow(() -> new NotFoundException("Check-in record not found"));
        requireOwner(checkIn.getSalesperson(), salespersonId);
        requirePending(schedule);
        if (checkIn.getCheckInTime() == null || checkIn.getCheckOutTime() != null) {
            throw new BusinessRuleException("Visit is not open for check-out");
        }
        if (dto.getOutcome() == null) throw new IllegalArgumentException("Visit outcome is required");
        requireCustomer(checkIn.getCustomer(), schedule.getCustomer());
        validateLinks(dto, orgId, schedule.getCustomer());

        checkIn.setCheckOutTime(LocalDateTime.now());
        checkIn.setOutcome(dto.getOutcome());
        checkIn.setNotes(dto.getNotes());
        checkIn.setSaleId(dto.getSaleId());
        checkIn.setPaymentId(dto.getPaymentId());

        visitCheckInRepo.save(checkIn);

        // Mark the schedule as COMPLETED
        schedule.setStatus(VisitStatus.COMPLETED);
        visitScheduleRepo.save(schedule);

        return checkIn;
    }

    private Long currentOrganization() {
        Long orgId = TenantContext.getCurrentTenant();
        if (orgId == null || orgId <= 0) throw new AccessDeniedException("Select an organization first");
        return orgId;
    }

    private void requireOwner(User owner, Long actorId) {
        if (actorId == null || owner == null || !actorId.equals(owner.getId())) {
            throw new AccessDeniedException("Only the assigned salesperson can update this visit");
        }
    }

    private void requirePending(VisitSchedule schedule) {
        if (schedule.getStatus() != VisitStatus.PENDING) {
            throw new BusinessRuleException("Only pending visits can be checked in or completed");
        }
    }

    private void requireScheduledToday(VisitSchedule schedule) {
        LocalDate today = LocalDate.now();
        if (schedule.getScheduledDate() == null || !schedule.getScheduledDate().equals(today)) {
            throw new BusinessRuleException(
                    "Visits can only be checked in on their scheduled date (" + schedule.getScheduledDate() + ")");
        }
    }

    private void requireCustomer(Customer actual, Customer expected) {
        if (actual == null || expected == null || expected.getId() == null ||
                !expected.getId().equals(actual.getId())) {
            throw new IllegalArgumentException("Linked record must belong to this visit's customer");
        }
    }

    private Sale visitSale(Long saleId, Long orgId, Customer customer) {
        Sale sale = saleRepo.findByIdAndOrganizationId(saleId, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Linked sale is not in the selected organization"));
        requireCustomer(sale.getCustomer(), customer);
        return sale;
    }

    private void validateLinks(CheckOutRequestDto dto, Long orgId, Customer customer) {
        if (dto.getSaleId() != null) visitSale(dto.getSaleId(), orgId, customer);
        if (dto.getPaymentId() == null) return;
        Payment payment = paymentRepo.findByIdAndOrganizationId(dto.getPaymentId(), orgId)
                .orElseThrow(() -> new IllegalArgumentException("Linked payment is not in the selected organization"));
        if (payment.getReferenceType() == ReferenceType.CUSTOMER) {
            if (customer == null || customer.getId() == null || !customer.getId().equals(payment.getReferenceId())) {
                throw new IllegalArgumentException("Linked payment must belong to this visit's customer");
            }
        } else if (payment.getReferenceType() == ReferenceType.SALE) {
            visitSale(payment.getReferenceId(), orgId, customer);
            if (dto.getSaleId() != null && !dto.getSaleId().equals(payment.getReferenceId())) {
                throw new IllegalArgumentException("Linked payment must reference the selected sale");
            }
        } else {
            throw new IllegalArgumentException("Only customer or sale payments can be linked to a visit");
        }
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
