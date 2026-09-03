package com.riceerp.backend.controller;

import com.riceerp.backend.dto.DeliveryConfirmRequest;
import com.riceerp.backend.dto.DeliveryCreateRequest;
import com.riceerp.backend.dto.DeliveryFailRequest;
import com.riceerp.backend.entity.Delivery;
import com.riceerp.backend.enums.DeliveryStatus;
import com.riceerp.backend.service.DeliveryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Delivery> createDeliveryNote(@Valid @RequestBody DeliveryCreateRequest request) {
        Delivery delivery = deliveryService.createDeliveryNote(request);
        return ResponseEntity.ok(delivery);
    }

    @GetMapping
    public List<Delivery> listDeliveries(@RequestParam(required = false) DeliveryStatus status) {
        return deliveryService.listDeliveries(status);
    }

    @GetMapping("/my-deliveries")
    public List<Delivery> getMyDeliveries(@RequestParam(required = false) DeliveryStatus status, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getPrincipal().toString());
        return deliveryService.getMyDeliveries(userId, status);
    }

    @GetMapping("/{id}")
    public Delivery getDeliveryById(@PathVariable Long id) {
        return deliveryService.getDeliveryById(id);
    }

    @PutMapping("/{id}/start")
    public Delivery startDelivery(@PathVariable Long id) {
        return deliveryService.startDelivery(id);
    }

    @RequestMapping(value = "/{id}/confirm", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<Delivery> confirmDelivery(@PathVariable Long id, @Valid @RequestBody DeliveryConfirmRequest request) {
        Delivery delivery = deliveryService.confirmDelivery(id, request);
        return ResponseEntity.ok(delivery);
    }

    @RequestMapping(value = "/{id}/fail", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<Delivery> markDeliveryFailed(@PathVariable Long id, @Valid @RequestBody DeliveryFailRequest request) {
        Delivery delivery = deliveryService.markDeliveryFailed(id, request);
        return ResponseEntity.ok(delivery);
    }
}
