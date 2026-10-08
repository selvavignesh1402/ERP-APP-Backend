package com.riceerp.backend.controller;

import com.riceerp.backend.dto.*;

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
    public ResponseEntity<DeliveryResponse> createDeliveryNote(@Valid @RequestBody DeliveryCreateRequest request) {
        Delivery delivery = deliveryService.createDeliveryNote(request);
        return ResponseEntity.ok(DeliveryResponse.from(delivery));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('delivery:view')")
    public List<DeliveryResponse> listDeliveries(@RequestParam(required = false) DeliveryStatus status) {
        return deliveryService.listDeliveries(status).stream().map(DeliveryResponse::from).toList();
    }

    @GetMapping("/my-deliveries")
    @PreAuthorize("hasAuthority('delivery:view')")
    public List<DeliveryResponse> getMyDeliveries(@RequestParam(required = false) DeliveryStatus status, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getPrincipal().toString());
        return deliveryService.getMyDeliveries(userId, status).stream().map(DeliveryResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('delivery:view')")
    public DeliveryResponse getDeliveryById(@PathVariable Long id) {
        return DeliveryResponse.from(deliveryService.getDeliveryById(id));
    }

    @PutMapping("/{id}/start")
    @PreAuthorize("hasAuthority('delivery:confirm') or hasAuthority('delivery:fail')")
    public DeliveryResponse startDelivery(@PathVariable Long id, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getPrincipal().toString());
        boolean isPrivileged = isPrivileged(authentication);
        return DeliveryResponse.from(deliveryService.startDelivery(id, userId, isPrivileged));
    }

    @RequestMapping(value = "/{id}/confirm", method = {RequestMethod.POST, RequestMethod.PUT})
    @PreAuthorize("hasAuthority('delivery:confirm')")
    public ResponseEntity<DeliveryResponse> confirmDelivery(@PathVariable Long id, @Valid @RequestBody DeliveryConfirmRequest request, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getPrincipal().toString());
        boolean isPrivileged = isPrivileged(authentication);
        Delivery delivery = deliveryService.confirmDelivery(id, request, userId, isPrivileged);
        return ResponseEntity.ok(DeliveryResponse.from(delivery));
    }

    @RequestMapping(value = "/{id}/fail", method = {RequestMethod.POST, RequestMethod.PUT})
    @PreAuthorize("hasAuthority('delivery:fail')")
    public ResponseEntity<DeliveryResponse> markDeliveryFailed(@PathVariable Long id, @Valid @RequestBody DeliveryFailRequest request, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getPrincipal().toString());
        boolean isPrivileged = isPrivileged(authentication);
        Delivery delivery = deliveryService.markDeliveryFailed(id, request, userId, isPrivileged);
        return ResponseEntity.ok(DeliveryResponse.from(delivery));
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
