package com.riceerp.backend.controller;

import com.riceerp.backend.entity.ReconciliationResult;
import com.riceerp.backend.service.ReconciliationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reconciliations")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('reconciliation:create')")
    public ReconciliationResult reconcile(@RequestParam Long purchaseId, @RequestParam Long invoiceId) {
        return reconciliationService.reconcile(purchaseId, invoiceId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('reconciliation:view')")
    public ReconciliationResult getById(@PathVariable Long id) {
        return reconciliationService.getById(id);
    }

    @GetMapping("/purchase/{purchaseId}")
    @PreAuthorize("hasAuthority('reconciliation:view')")
    public ReconciliationResult getForPurchase(@PathVariable Long purchaseId) {
        return reconciliationService.getForPurchase(purchaseId);
    }
}