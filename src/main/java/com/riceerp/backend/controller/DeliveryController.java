package com.riceerp.backend.controller;

import com.riceerp.backend.dto.DeliveryConfirmRequest;
import com.riceerp.backend.dto.DeliveryCreateRequest;
import com.riceerp.backend.dto.DeliveryFailRequest;
import com.riceerp.backend.entity.Delivery;
import com.riceerp.backend.enums.DeliveryStatus;
import com.riceerp.backend.service.DeliveryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/deliveries")
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('delivery:create') or hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Delivery> createDeliveryNote(@Valid @RequestBody DeliveryCreateRequest request) {
        Delivery delivery = deliveryService.createDeliveryNote(request);
        return ResponseEntity.ok(delivery);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('delivery:view')")
    public List<Delivery> listDeliveries(@RequestParam(required = false) DeliveryStatus status) {
        return deliveryService.listDeliveries(status);
    }

    @GetMapping("/my-deliveries")
    @PreAuthorize("hasAuthority('delivery:view')")
    public List<Delivery> getMyDeliveries(@RequestParam(required = false) DeliveryStatus status, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getPrincipal().toString());
        return deliveryService.getMyDeliveries(userId, status);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('delivery:view')")
    public Delivery getDeliveryById(@PathVariable Long id) {
        return deliveryService.getDeliveryById(id);
    }

    @PutMapping("/{id}/start")
    @PreAuthorize("hasAuthority('delivery:confirm') or hasAuthority('delivery:fail')")
    public Delivery startDelivery(@PathVariable Long id, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getPrincipal().toString());
        boolean isPrivileged = isPrivileged(authentication);
        return deliveryService.startDelivery(id, userId, isPrivileged);
    }

    @RequestMapping(value = "/{id}/confirm", method = {RequestMethod.POST, RequestMethod.PUT})
    @PreAuthorize("hasAuthority('delivery:confirm')")
    public ResponseEntity<Delivery> confirmDelivery(@PathVariable Long id, @Valid @RequestBody DeliveryConfirmRequest request, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getPrincipal().toString());
        boolean isPrivileged = isPrivileged(authentication);
        Delivery delivery = deliveryService.confirmDelivery(id, request, userId, isPrivileged);
        return ResponseEntity.ok(delivery);
    }

    @RequestMapping(value = "/{id}/fail", method = {RequestMethod.POST, RequestMethod.PUT})
    @PreAuthorize("hasAuthority('delivery:fail')")
    public ResponseEntity<Delivery> markDeliveryFailed(@PathVariable Long id, @Valid @RequestBody DeliveryFailRequest request, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getPrincipal().toString());
        boolean isPrivileged = isPrivileged(authentication);
        Delivery delivery = deliveryService.markDeliveryFailed(id, request, userId, isPrivileged);
        return ResponseEntity.ok(delivery);
    }

    private boolean isPrivileged(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMIN") ||
                a.getAuthority().equals("ROLE_ORG_ADMIN") ||
                a.getAuthority().equals("ROLE_MANAGER") ||
                a.getAuthority().equals("ROLE_PLATFORM_MASTER_ADMIN") ||
                a.getAuthority().equals("ROLE_MASTER_ADMIN")
        );
    }
}
