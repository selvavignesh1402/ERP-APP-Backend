package com.riceerp.backend.controller;

import com.riceerp.backend.dto.CheckInRequestDto;
import com.riceerp.backend.dto.CheckOutRequestDto;
import com.riceerp.backend.entity.VisitCheckIn;
import com.riceerp.backend.service.VisitService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/visits")
public class VisitController {

    private final VisitService visitService;

    public VisitController(VisitService visitService) {
        this.visitService = visitService;
    }

    // Salesperson: Check-in at a shop
    @PostMapping("/{scheduleId}/check-in")
    @PreAuthorize("hasAuthority('beat-plan:view')")
    public ResponseEntity<VisitCheckIn> checkIn(
            @PathVariable Long scheduleId,
            @Valid @RequestBody CheckInRequestDto dto,
            Authentication auth) {
        Long salespersonId = getCurrentUserId(auth);
        return ResponseEntity.ok(visitService.checkIn(scheduleId, salespersonId, dto));
    }

    // Salesperson: Complete visit (check-out with outcome)
    @PutMapping("/{checkInId}/check-out")
    @PreAuthorize("hasAuthority('beat-plan:view')")
    public ResponseEntity<VisitCheckIn> checkOut(
            @PathVariable Long checkInId,
            @Valid @RequestBody CheckOutRequestDto dto) {
        return ResponseEntity.ok(visitService.checkOut(checkInId, dto));
    }

    // Visit history for a customer
    @GetMapping("/customer/{customerId}/history")
    @PreAuthorize("hasAuthority('beat-plan:view') or hasAuthority('customer:view')")
    public ResponseEntity<List<VisitCheckIn>> getVisitHistory(@PathVariable Long customerId) {
        return ResponseEntity.ok(visitService.getVisitHistory(customerId));
    }

    // History for a salesperson
    @GetMapping("/salesperson/{salespersonId}/history")
    @PreAuthorize("hasAuthority('beat-plan:view') or hasAuthority('beat-plan:manage')")
    public ResponseEntity<List<VisitCheckIn>> getSalespersonHistory(@PathVariable Long salespersonId) {
        return ResponseEntity.ok(visitService.getSalespersonHistory(salespersonId));
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
