package com.riceerp.backend.service;

import com.riceerp.backend.dto.BeatPlanDto;
import com.riceerp.backend.dto.ManagerDashboardDto;
import com.riceerp.backend.dto.TodayRouteDto;
import com.riceerp.backend.entity.*;
import com.riceerp.backend.enums.Role;
import com.riceerp.backend.enums.VisitStatus;
import com.riceerp.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BeatPlanService {

    @Autowired
    private BeatPlanRepository beatPlanRepo;
    @Autowired
    private BeatPlanEntryRepository beatPlanEntryRepo;
    @Autowired
    private VisitScheduleRepository visitScheduleRepo;
    @Autowired
    private VisitCheckInRepository visitCheckInRepo;
    @Autowired
    private UserRepository userRepo;
    @Autowired
    private CustomerRepository customerRepo;
    @Autowired
    private SaleRepository saleRepo;

    // ─────────────────────────────────────────────
    // CREATE / UPDATE BEAT PLANS
    // ─────────────────────────────────────────────

    @Transactional
    public BeatPlanDto createBeatPlan(BeatPlanDto dto) {
        User salesperson = userRepo.findById(dto.getSalespersonId())
                .orElseThrow(() -> new RuntimeException("Salesperson not found: " + dto.getSalespersonId()));

        BeatPlan plan = new BeatPlan();
        plan.setName(dto.getName());
        plan.setSalesperson(salesperson);
        plan.setActive(true);
        beatPlanRepo.save(plan);

        if (dto.getEntries() != null) {
            for (BeatPlanDto.EntryDto e : dto.getEntries()) {
                Customer customer = customerRepo.findById(e.getCustomerId())
                        .orElseThrow(() -> new RuntimeException("Customer not found: " + e.getCustomerId()));
                BeatPlanEntry entry = new BeatPlanEntry();
                entry.setBeatPlan(plan);
                entry.setDayOfWeek(e.getDayOfWeek());
                entry.setCustomer(customer);
                entry.setVisitOrder(e.getVisitOrder());
                beatPlanEntryRepo.save(entry);
            }
        }
        return toDto(plan);
    }

    @Transactional
    public BeatPlanDto updateBeatPlan(Long planId, BeatPlanDto dto) {
        BeatPlan plan = beatPlanRepo.findById(planId)
                .orElseThrow(() -> new RuntimeException("Beat plan not found: " + planId));
        plan.setName(dto.getName());
        plan.setActive(dto.isActive());

        // Replace entries
        beatPlanEntryRepo.deleteByBeatPlanId(planId);
        if (dto.getEntries() != null) {
            for (BeatPlanDto.EntryDto e : dto.getEntries()) {
                Customer customer = customerRepo.findById(e.getCustomerId())
                        .orElseThrow(() -> new RuntimeException("Customer not found: " + e.getCustomerId()));
                BeatPlanEntry entry = new BeatPlanEntry();
                entry.setBeatPlan(plan);
                entry.setDayOfWeek(e.getDayOfWeek());
                entry.setCustomer(customer);
                entry.setVisitOrder(e.getVisitOrder());
                beatPlanEntryRepo.save(entry);
            }
        }
        beatPlanRepo.save(plan);
        return toDto(plan);
    }

    // ─────────────────────────────────────────────
    // LIST PLANS
    // ─────────────────────────────────────────────

    public List<BeatPlanDto> getAllPlans() {
        return beatPlanRepo.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<BeatPlanDto> getPlansForSalesperson(Long salespersonId) {
        return beatPlanRepo.findBySalespersonId(salespersonId).stream().map(this::toDto).collect(Collectors.toList());
    }

    public BeatPlanDto getPlanById(Long id) {
        return beatPlanRepo.findById(id).map(this::toDto)
                .orElseThrow(() -> new RuntimeException("Beat plan not found: " + id));
    }

    // ─────────────────────────────────────────────
    // GENERATE WEEKLY SCHEDULES
    // ─────────────────────────────────────────────

    @Transactional
    public int generateWeeklySchedules(LocalDate weekStart) {
        // weekStart should be a Monday
        List<BeatPlan> activePlans = beatPlanRepo.findByIsActiveTrue();
        int created = 0;

        for (BeatPlan plan : activePlans) {
            List<BeatPlanEntry> entries = beatPlanEntryRepo.findByBeatPlanId(plan.getId());
            for (BeatPlanEntry entry : entries) {
                // Compute the actual date for this day-of-week in the given week
                LocalDate visitDate = weekStart;
                while (visitDate.getDayOfWeek() != entry.getDayOfWeek()) {
                    visitDate = visitDate.plusDays(1);
                }
                // Idempotent — skip if already scheduled
                if (!visitScheduleRepo.existsByBeatPlanIdAndScheduledDate(plan.getId(), visitDate)) {
                    VisitSchedule schedule = new VisitSchedule();
                    schedule.setBeatPlan(plan);
                    schedule.setSalesperson(plan.getSalesperson());
                    schedule.setCustomer(entry.getCustomer());
                    schedule.setScheduledDate(visitDate);
                    schedule.setVisitOrder(entry.getVisitOrder());
                    schedule.setStatus(VisitStatus.PENDING);
                    visitScheduleRepo.save(schedule);
                    created++;
                }
            }
        }
        return created;
    }

    // ─────────────────────────────────────────────
    // TODAY'S ROUTE — for salesperson
    // ─────────────────────────────────────────────

    public List<TodayRouteDto> getTodayRoute(Long salespersonId) {
        LocalDate today = LocalDate.now();
        List<VisitSchedule> schedules = visitScheduleRepo
                .findBySalespersonIdAndScheduledDateOrderByVisitOrderAsc(salespersonId, today);

        return schedules.stream().map(s -> {
            TodayRouteDto dto = new TodayRouteDto();
            dto.setScheduleId(s.getId());
            dto.setCustomerId(s.getCustomer().getId());
            dto.setCustomerName(s.getCustomer().getCustomerName());
            dto.setCustomerPhone(s.getCustomer().getPhone());
            dto.setCustomerAddress(s.getCustomer().getAddress());
            dto.setCreditLimit(s.getCustomer().getCreditLimit());
            dto.setOutstandingBalance(s.getCustomer().getCreditBalance());
            dto.setVisitOrder(s.getVisitOrder());
            dto.setStatus(s.getStatus());

            // Enrich with last visit info
            visitCheckInRepo.findByCustomerIdOrderByCheckInTimeDesc(s.getCustomer().getId())
                    .stream().findFirst().ifPresent(ci -> {
                        dto.setLastVisitDate(ci.getCheckInTime() != null ? ci.getCheckInTime().toLocalDate() : null);
                    });

            // Enrich with last order
            saleRepo.findTop5ByOrderBySaleDateDesc().stream()
                    .filter(sale -> sale.getCustomer() != null &&
                            sale.getCustomer().getId().equals(s.getCustomer().getId()))
                    .findFirst().ifPresent(sale -> {
                        dto.setLastOrderAmount(sale.getGrandTotal());
                        dto.setLastOrderDate(sale.getSaleDate().toLocalDate().toString());
                    });

            // Check if already checked in today
            visitCheckInRepo.findByVisitScheduleId(s.getId()).ifPresent(ci -> {
                dto.setCheckInId(ci.getId());
                dto.setCheckInTime(ci.getCheckInTime());
            });

            return dto;
        }).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // MANAGER DASHBOARD
    // ─────────────────────────────────────────────

    public ManagerDashboardDto getManagerDashboard(LocalDate date) {
        List<VisitSchedule> allSchedules = visitScheduleRepo.findAllForDate(date);

        // Group by salesperson
        Map<Long, List<VisitSchedule>> bySalesperson = allSchedules.stream()
                .collect(Collectors.groupingBy(s -> s.getSalesperson().getId()));

        List<ManagerDashboardDto.SalespersonSummary> team = new ArrayList<>();
        List<ManagerDashboardDto.AlertDto> alerts = new ArrayList<>();

        for (Map.Entry<Long, List<VisitSchedule>> entry : bySalesperson.entrySet()) {
            Long spId = entry.getKey();
            List<VisitSchedule> spSchedules = entry.getValue();

            ManagerDashboardDto.SalespersonSummary summary = new ManagerDashboardDto.SalespersonSummary();
            summary.setSalespersonId(spId);
            summary.setSalespersonName(spSchedules.get(0).getSalesperson().getName());
            summary.setTotalScheduled(spSchedules.size());
            summary.setCompleted(spSchedules.stream().filter(s -> s.getStatus() == VisitStatus.COMPLETED).count());
            summary.setMissed(spSchedules.stream().filter(s -> s.getStatus() == VisitStatus.MISSED).count());
            summary.setPending(spSchedules.stream().filter(s -> s.getStatus() == VisitStatus.PENDING).count());

            // Sum orders & collections from check-ins today
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);
            List<VisitCheckIn> checkIns = visitCheckInRepo.findBySalespersonIdAndCheckInTimeBetween(spId, dayStart,
                    dayEnd);

            double totalOrders = checkIns.stream()
                    .filter(ci -> ci.getSaleId() != null)
                    .mapToDouble(ci -> saleRepo.findById(ci.getSaleId()).map(Sale::getGrandTotal).orElse(0.0))
                    .sum();
            summary.setTotalOrders(totalOrders);
            summary.setTotalCollections(0.0); // Payment repo query can be added here

            team.add(summary);

            // Smart alert: missed 3+ scheduled visits
            if (summary.getMissed() >= 3) {
                ManagerDashboardDto.AlertDto alert = new ManagerDashboardDto.AlertDto();
                alert.setType("DANGER");
                alert.setMessage(
                        summary.getSalespersonName() + " missed " + summary.getMissed() + " scheduled visits today");
                alert.setSalespersonId(spId);
                alerts.add(alert);
            }
        }

        ManagerDashboardDto result = new ManagerDashboardDto();
        result.setTeam(team);
        result.setAlerts(alerts);
        return result;
    }

    // ─────────────────────────────────────────────
    // MAPPER
    // ─────────────────────────────────────────────

    private BeatPlanDto toDto(BeatPlan plan) {
        BeatPlanDto dto = new BeatPlanDto();
        dto.setId(plan.getId());
        dto.setName(plan.getName());
        dto.setSalespersonId(plan.getSalesperson().getId());
        dto.setSalespersonName(plan.getSalesperson().getName());
        dto.setActive(plan.isActive());

        List<BeatPlanEntry> entries = beatPlanEntryRepo.findByBeatPlanId(plan.getId());
        dto.setEntries(entries.stream().map(e -> {
            BeatPlanDto.EntryDto edto = new BeatPlanDto.EntryDto();
            edto.setId(e.getId());
            edto.setDayOfWeek(e.getDayOfWeek());
            edto.setCustomerId(e.getCustomer().getId());
            edto.setCustomerName(e.getCustomer().getCustomerName());
            edto.setVisitOrder(e.getVisitOrder());
            return edto;
        }).collect(Collectors.toList()));

        return dto;
    }
}
