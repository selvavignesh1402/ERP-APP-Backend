package com.riceerp.backend.controller;

import com.riceerp.backend.dto.BeatPlanDto;
import com.riceerp.backend.dto.ManagerDashboardDto;
import com.riceerp.backend.dto.TodayRouteDto;
import com.riceerp.backend.service.BeatPlanService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<BeatPlanDto> createBeatPlan(@RequestBody BeatPlanDto dto) {
        return ResponseEntity.ok(beatPlanService.createBeatPlan(dto));
    }

    // Manager: List all beat plans
    @GetMapping
    public ResponseEntity<List<BeatPlanDto>> getAllPlans() {
        return ResponseEntity.ok(beatPlanService.getAllPlans());
    }

    // Manager: Get one beat plan
    @GetMapping("/{id}")
    public ResponseEntity<BeatPlanDto> getPlanById(@PathVariable Long id) {
        return ResponseEntity.ok(beatPlanService.getPlanById(id));
    }

    // Manager: Update a beat plan
    @PutMapping("/{id}")
    public ResponseEntity<BeatPlanDto> updateBeatPlan(@PathVariable Long id, @RequestBody BeatPlanDto dto) {
        return ResponseEntity.ok(beatPlanService.updateBeatPlan(id, dto));
    }

    // Manager: List plans for a specific salesperson
    @GetMapping("/salesperson/{salespersonId}")
    public ResponseEntity<List<BeatPlanDto>> getPlansForSalesperson(@PathVariable Long salespersonId) {
        return ResponseEntity.ok(beatPlanService.getPlansForSalesperson(salespersonId));
    }

    // Manager: Generate weekly schedules (idempotent)
    @PostMapping("/generate-week")
    public ResponseEntity<Map<String, Object>> generateWeek(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        int count = beatPlanService.generateWeeklySchedules(weekStart);
        return ResponseEntity.ok(Map.of("schedulesCreated", count, "weekStart", weekStart.toString()));
    }

    // Salesperson: Get today's route (uses JWT to identify user)
    @GetMapping("/my-route")
    public ResponseEntity<List<TodayRouteDto>> getMyRoute(Authentication auth) {
        // Auth principal holds phone number; we look up userId from SecurityContext
        // For simplicity we accept salespersonId as query param here
        // (integrate with your existing JWT / SecurityContext pattern)
        return ResponseEntity.ok(beatPlanService.getTodayRoute(getCurrentUserId(auth)));
    }

    // Salesperson: Get route for a specific date
    @GetMapping("/route/{salespersonId}")
    public ResponseEntity<List<TodayRouteDto>> getRouteForSalesperson(
            @PathVariable Long salespersonId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(beatPlanService.getTodayRoute(salespersonId));
    }

    // Manager: Live dashboard
    @GetMapping("/manager-dashboard")
    public ResponseEntity<ManagerDashboardDto> getManagerDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(beatPlanService.getManagerDashboard(targetDate));
    }

    // ─── helper: extract user id from Spring Security principal ───
    private Long getCurrentUserId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new RuntimeException("Not authenticated");
        }
        // Integrate with your existing UserDetailsService that stores the User entity
        // The pattern below works with Spring UserDetails where username = phone number
        // Replace with your actual implementation if different
        Object principal = auth.getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
            // Look up by phone — adjust to match your existing auth pattern
            try {
                return Long.parseLong(ud.getUsername());
            } catch (NumberFormatException ignored) {
            }
        }
        throw new RuntimeException("Cannot resolve user id from authentication");
    }
}
