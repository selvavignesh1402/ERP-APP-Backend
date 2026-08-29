package com.riceerp.backend.controller;

import com.riceerp.backend.dto.CheckInRequestDto;
import com.riceerp.backend.dto.CheckOutRequestDto;
import com.riceerp.backend.entity.VisitCheckIn;
import com.riceerp.backend.service.VisitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/visits")
public class VisitController {

    @Autowired
    private VisitService visitService;

    // Salesperson: Check-in at a shop
    @PostMapping("/{scheduleId}/check-in")
    public ResponseEntity<VisitCheckIn> checkIn(
            @PathVariable Long scheduleId,
            @RequestBody CheckInRequestDto dto,
            Authentication auth) {
        Long salespersonId = getCurrentUserId(auth);
        return ResponseEntity.ok(visitService.checkIn(scheduleId, salespersonId, dto));
    }

    // Salesperson: Complete visit (check-out with outcome)
    @PutMapping("/{checkInId}/check-out")
    public ResponseEntity<VisitCheckIn> checkOut(
            @PathVariable Long checkInId,
            @RequestBody CheckOutRequestDto dto) {
        return ResponseEntity.ok(visitService.checkOut(checkInId, dto));
    }

    // Any role: Visit history for a customer
    @GetMapping("/customer/{customerId}/history")
    public ResponseEntity<List<VisitCheckIn>> getVisitHistory(@PathVariable Long customerId) {
        return ResponseEntity.ok(visitService.getVisitHistory(customerId));
    }

    // Manager/Salesperson: History for a salesperson
    @GetMapping("/salesperson/{salespersonId}/history")
    public ResponseEntity<List<VisitCheckIn>> getSalespersonHistory(@PathVariable Long salespersonId) {
        return ResponseEntity.ok(visitService.getSalespersonHistory(salespersonId));
    }

    private Long getCurrentUserId(Authentication auth) {
        if (auth == null)
            throw new RuntimeException("Not authenticated");
        Object principal = auth.getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
            try {
                return Long.parseLong(ud.getUsername());
            } catch (NumberFormatException ignored) {
            }
        }
        throw new RuntimeException("Cannot resolve user id from authentication");
    }
}
