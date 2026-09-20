package com.riceerp.backend.controller;

import com.riceerp.backend.dto.BeatPlanDto;
import com.riceerp.backend.dto.ManagerDashboardDto;
import com.riceerp.backend.dto.TodayRouteDto;
import com.riceerp.backend.service.BeatPlanService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/beat-plans")
public class BeatPlanController {

    private final BeatPlanService beatPlanService;

    public BeatPlanController(BeatPlanService beatPlanService) {
        this.beatPlanService = beatPlanService;
    }

    // Manager: Create a beat plan
    @PostMapping
    @PreAuthorize("hasAuthority('beat-plan:manage')")
    public ResponseEntity<BeatPlanDto> createBeatPlan(@Valid @RequestBody BeatPlanDto dto) {
        return ResponseEntity.ok(beatPlanService.createBeatPlan(dto));
    }

    // Manager: List all beat plans
    @GetMapping
    @PreAuthorize("hasAuthority('beat-plan:manage') or hasAuthority('beat-plan:view')")
    public ResponseEntity<List<BeatPlanDto>> getAllPlans() {
        return ResponseEntity.ok(beatPlanService.getAllPlans());
    }

    // Manager: Get one beat plan
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('beat-plan:manage') or hasAuthority('beat-plan:view')")
    public ResponseEntity<BeatPlanDto> getPlanById(@PathVariable Long id) {
        return ResponseEntity.ok(beatPlanService.getPlanById(id));
    }

    // Manager: Update a beat plan
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('beat-plan:manage')")
    public ResponseEntity<BeatPlanDto> updateBeatPlan(@PathVariable Long id, @Valid @RequestBody BeatPlanDto dto) {
        return ResponseEntity.ok(beatPlanService.updateBeatPlan(id, dto));
    }

    // Manager: List plans for a specific salesperson
    @GetMapping("/salesperson/{salespersonId}")
    @PreAuthorize("hasAuthority('beat-plan:manage') or hasAuthority('beat-plan:view')")
    public ResponseEntity<List<BeatPlanDto>> getPlansForSalesperson(@PathVariable Long salespersonId) {
        return ResponseEntity.ok(beatPlanService.getPlansForSalesperson(salespersonId));
    }

    // Manager: Generate weekly schedules (idempotent)
    @PostMapping("/generate-week")
    @PreAuthorize("hasAuthority('beat-plan:manage')")
    public ResponseEntity<Map<String, Object>> generateWeek(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        int count = beatPlanService.generateWeeklySchedules(weekStart);
        return ResponseEntity.ok(Map.of("schedulesCreated", count, "weekStart", weekStart.toString()));
    }

    // Salesperson: Get today's route
    @GetMapping("/my-route")
    @PreAuthorize("hasAuthority('beat-plan:view')")
    public ResponseEntity<List<TodayRouteDto>> getMyRoute(Authentication auth) {
        return ResponseEntity.ok(beatPlanService.getTodayRoute(getCurrentUserId(auth)));
    }

    // Salesperson / Manager: Get route for a specific date
    @GetMapping("/route/{salespersonId}")
    @PreAuthorize("hasAuthority('beat-plan:manage') or hasAuthority('beat-plan:view')")
    public ResponseEntity<List<TodayRouteDto>> getRouteForSalesperson(
            @PathVariable Long salespersonId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(beatPlanService.getTodayRoute(salespersonId));
    }

    // Manager: Live dashboard
    @GetMapping("/manager-dashboard")
    @PreAuthorize("hasAuthority('beat-plan:manage') or hasAuthority('beat-plan:view')")
    public ResponseEntity<ManagerDashboardDto> getManagerDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(beatPlanService.getManagerDashboard(targetDate));
    }

    private Long getCurrentUserId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new RuntimeException("Not authenticated");
        }
        try {
            return Long.parseLong(auth.getPrincipal().toString());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Cannot resolve user id from authentication");
        }
    }
}
