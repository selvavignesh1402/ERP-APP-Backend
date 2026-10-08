package com.riceerp.backend.service;

import com.riceerp.backend.dto.BeatPlanDto;
import com.riceerp.backend.dto.ManagerDashboardDto;
import com.riceerp.backend.dto.TodayRouteDto;
import com.riceerp.backend.entity.*;
import com.riceerp.backend.enums.PlatformRole;
import com.riceerp.backend.enums.VisitStatus;
import com.riceerp.backend.repository.*;
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

    private final BeatPlanRepository beatPlanRepo;
    private final BeatPlanEntryRepository beatPlanEntryRepo;
    private final VisitScheduleRepository visitScheduleRepo;
    private final VisitCheckInRepository visitCheckInRepo;
    private final UserRepository userRepo;
    private final CustomerRepository customerRepo;
    private final SaleRepository saleRepo;
    private final PaymentRepository paymentRepo;

    public BeatPlanService(
            BeatPlanRepository beatPlanRepo,
            BeatPlanEntryRepository beatPlanEntryRepo,
            VisitScheduleRepository visitScheduleRepo,
            VisitCheckInRepository visitCheckInRepo,
            UserRepository userRepo,
            CustomerRepository customerRepo,
            SaleRepository saleRepo,
            PaymentRepository paymentRepo) {
        this.beatPlanRepo = beatPlanRepo;
        this.beatPlanEntryRepo = beatPlanEntryRepo;
        this.visitScheduleRepo = visitScheduleRepo;
        this.visitCheckInRepo = visitCheckInRepo;
        this.userRepo = userRepo;
        this.customerRepo = customerRepo;
        this.saleRepo = saleRepo;
        this.paymentRepo = paymentRepo;
    }

    // ─────────────────────────────────────────────
    // CREATE / UPDATE BEAT PLANS
    // ─────────────────────────────────────────────

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public BeatPlanDto createBeatPlan(BeatPlanDto dto) {
        lockPlans(); validateEntries(dto);
        User salesperson = userRepo.findActiveOrganizationUser(dto.getSalespersonId(), tenant())
                .orElseThrow(() -> new com.riceerp.backend.exception.BusinessRuleException("Salesperson not found: " + dto.getSalespersonId()));

        BeatPlan plan = new BeatPlan();
        plan.setName(dto.getName());
        plan.setSalesperson(salesperson);
        plan.setActive(true);
        beatPlanRepo.save(plan);

        if (dto.getEntries() != null) {
            for (BeatPlanDto.EntryDto e : dto.getEntries()) {
                Customer customer = customerRepo.findById(e.getCustomerId())
                        .orElseThrow(() -> new com.riceerp.backend.exception.BusinessRuleException("Customer not found: " + e.getCustomerId()));
                BeatPlanEntry entry = new BeatPlanEntry();
                entry.setBeatPlan(plan);
                entry.setDayOfWeek(e.getDayOfWeek());
                entry.setCustomer(customer);
                entry.setVisitOrder(e.getVisitOrder());
                beatPlanEntryRepo.save(entry);
            }
        }
        // Sync schedules for the current week immediately
        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
        generateWeeklySchedules(monday);
        ensureSchedulesForDate(plan.getSalesperson().getId(), LocalDate.now());
        return toDto(plan);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public BeatPlanDto updateBeatPlan(Long planId, BeatPlanDto dto) {
        lockPlans(); validateEntries(dto);
        BeatPlan plan = beatPlanRepo.findById(planId)
                .orElseThrow(() -> new com.riceerp.backend.exception.BusinessRuleException("Beat plan not found: " + planId));
        plan.setName(dto.getName());
        plan.setActive(dto.isActive());
        if (dto.getSalespersonId() != null) plan.setSalesperson(userRepo.findActiveOrganizationUser(dto.getSalespersonId(), tenant())
                .orElseThrow(() -> new IllegalArgumentException("Salesperson must be an active member of this shop")));
        Set<LocalDate> futureDates = new HashSet<>();
        for (VisitSchedule schedule : visitScheduleRepo.findByBeatPlanIdAndScheduledDateGreaterThanEqual(planId, LocalDate.now())) {
            futureDates.add(schedule.getScheduledDate());
            // Keep completed or started visits as historical records.
            VisitSchedule locked = visitScheduleRepo.findForUpdate(schedule.getId(), tenant()).orElseThrow();
            if (locked.getStatus() == VisitStatus.PENDING && visitCheckInRepo.findByVisitScheduleId(locked.getId()).isEmpty())
                visitScheduleRepo.delete(locked);
        }
        visitScheduleRepo.flush();

        // Replace entries
        beatPlanEntryRepo.deleteByBeatPlanId(planId);
        if (dto.getEntries() != null) {
            for (BeatPlanDto.EntryDto e : dto.getEntries()) {
                Customer customer = customerRepo.findById(e.getCustomerId())
                        .orElseThrow(() -> new com.riceerp.backend.exception.BusinessRuleException("Customer not found: " + e.getCustomerId()));
                BeatPlanEntry entry = new BeatPlanEntry();
                entry.setBeatPlan(plan);
                entry.setDayOfWeek(e.getDayOfWeek());
                entry.setCustomer(customer);
                entry.setVisitOrder(e.getVisitOrder());
                beatPlanEntryRepo.save(entry);
            }
        }
        beatPlanRepo.save(plan);
        for (LocalDate date : futureDates) ensureSchedulesForDate(plan.getSalesperson().getId(), date);
        // Sync schedules for the current week immediately
        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
        generateWeeklySchedules(monday);
        ensureSchedulesForDate(plan.getSalesperson().getId(), LocalDate.now());
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
                .orElseThrow(() -> new com.riceerp.backend.exception.BusinessRuleException("Beat plan not found: " + id));
    }

    // ─────────────────────────────────────────────
    // GENERATE WEEKLY SCHEDULES & AUTO-ENSURE
    // ─────────────────────────────────────────────

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void ensureSchedulesForDate(Long salespersonId, LocalDate date) {
        lockPlans();
        visitScheduleRepo.expireUnstartedSchedules(tenant(), LocalDate.now());
        if (date.isBefore(LocalDate.now())) return;
        DayOfWeek targetDay = date.getDayOfWeek();
        List<BeatPlan> plans;
        if (salespersonId != null) {
            plans = beatPlanRepo.findBySalespersonId(salespersonId).stream()
                    .filter(BeatPlan::isActive)
                    .collect(Collectors.toList());
            // If no active plans assigned specifically to this salespersonId,
            // check if there are active plans in the system (e.g. for Admin user)
            if (plans.isEmpty()) {
                User user = userRepo.findById(salespersonId).orElse(null);
                if (user != null && (user.getPlatformRole() == PlatformRole.MASTER_ADMIN)) {
                    plans = beatPlanRepo.findByIsActiveTrue();
                }
            }
        } else {
            plans = beatPlanRepo.findByIsActiveTrue();
        }

        for (BeatPlan plan : plans) {
            List<BeatPlanEntry> entries = beatPlanEntryRepo.findByBeatPlanId(plan.getId());
            for (BeatPlanEntry entry : entries) {
                if (entry.getDayOfWeek() == targetDay) {
                    if (!visitScheduleRepo.existsByBeatPlanIdAndCustomerIdAndScheduledDate(
                            plan.getId(), entry.getCustomer().getId(), date)) {
                        VisitSchedule schedule = new VisitSchedule();
                        schedule.setBeatPlan(plan);
                        schedule.setSalesperson(plan.getSalesperson());
                        schedule.setCustomer(entry.getCustomer());
                        schedule.setScheduledDate(date);
                        schedule.setVisitOrder(entry.getVisitOrder());
                        schedule.setStatus(VisitStatus.PENDING);
                        visitScheduleRepo.save(schedule);
                    }
                }
            }
        }
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public int generateWeeklySchedules(LocalDate weekStart) {
        lockPlans();
        if (weekStart == null || weekStart.getDayOfWeek() != DayOfWeek.MONDAY) throw new IllegalArgumentException("Week start must be a Monday");
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
                if (visitDate.isBefore(LocalDate.now())) continue;
                // Idempotent — skip if already scheduled for this customer on this date
                if (!visitScheduleRepo.existsByBeatPlanIdAndCustomerIdAndScheduledDate(
                        plan.getId(), entry.getCustomer().getId(), visitDate)) {
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

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public List<TodayRouteDto> getTodayRoute(Long salespersonId) {
        return getRoute(salespersonId, LocalDate.now());
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public List<TodayRouteDto> getRoute(Long salespersonId, LocalDate today) {
        Objects.requireNonNull(today, "Route date is required");
        ensureSchedulesForDate(salespersonId, today);

        List<VisitSchedule> schedules = visitScheduleRepo
                .findBySalespersonIdAndScheduledDateOrderByVisitOrderAsc(salespersonId, today);

        // Fallback: If salesperson has no personal schedules, but is Admin/Manager, load all schedules for today
        if (schedules.isEmpty()) {
            User user = userRepo.findById(salespersonId).orElse(null);
            if (user != null && (user.getPlatformRole() == PlatformRole.MASTER_ADMIN)) {
                schedules = visitScheduleRepo.findAllForDate(today);
            }
        }

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
            dto.setScheduledDate(s.getScheduledDate());

            // Enrich with last visit info
            visitCheckInRepo.findByCustomerIdOrderByCheckInTimeDesc(s.getCustomer().getId())
                    .stream().findFirst().ifPresent(ci -> {
                        dto.setLastVisitDate(ci.getCheckInTime() != null ? ci.getCheckInTime().toLocalDate() : null);
                    });

            // Enrich with last order
            saleRepo.findFirstByCustomerIdOrderBySaleDateDescIdDesc(s.getCustomer().getId()).ifPresent(sale -> {
                        dto.setLastOrderAmount(sale.getGrandTotal());
                        dto.setLastOrderDate(sale.getSaleDate() == null ? null : sale.getSaleDate().toLocalDate().toString());
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

    @Transactional
    public ManagerDashboardDto getManagerDashboard(LocalDate date) {
        visitScheduleRepo.expireUnstartedSchedules(tenant(), LocalDate.now());
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

            java.math.BigDecimal totalOrders = checkIns.stream()
                    .filter(ci -> ci.getSaleId() != null)
                    .map(ci -> saleRepo.findById(ci.getSaleId()).map(Sale::getGrandTotal).orElse(java.math.BigDecimal.ZERO))
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            summary.setTotalOrders(totalOrders);

            // Collections = payments recorded against today's check-ins
            List<Long> paymentIds = checkIns.stream()
                    .map(VisitCheckIn::getPaymentId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            java.math.BigDecimal totalCollections = paymentIds.isEmpty()
                    ? java.math.BigDecimal.ZERO
                    : paymentRepo.sumAmountByIdIn(paymentIds);
            summary.setTotalCollections(totalCollections);

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

    private Long tenant() {
        Long id = com.riceerp.backend.security.TenantContext.getCurrentTenant();
        if (id == null || id <= 0) throw new org.springframework.security.access.AccessDeniedException("Select a shop first");
        return id;
    }
    private void lockPlans() { beatPlanRepo.lockOrganization(tenant()).orElseThrow(); }
    private void validateEntries(BeatPlanDto dto) {
        if (dto.getName() == null || dto.getName().isBlank()) throw new IllegalArgumentException("Plan name is required");
        Set<String> seen = new HashSet<>();
        if (dto.getEntries() != null) for (BeatPlanDto.EntryDto entry : dto.getEntries()) {
            if (entry == null || entry.getCustomerId() == null || entry.getDayOfWeek() == null || entry.getVisitOrder() < 0 ||
                    !seen.add(entry.getDayOfWeek() + ":" + entry.getCustomerId()))
                throw new IllegalArgumentException("Each customer/day must appear once with a non-negative visit order");
        }
    }

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
