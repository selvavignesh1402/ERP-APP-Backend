package com.riceerp.backend.controller;

import com.riceerp.backend.dto.*;

import com.riceerp.backend.entity.ReconciliationResult;
import com.riceerp.backend.service.ReconciliationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/reconciliations", "/reconciliations"})
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('reconciliation:create')")
    public ReconciliationResultResponse reconcile(@RequestParam Long purchaseId, @RequestParam Long invoiceId) {
        return ReconciliationResultResponse.from(reconciliationService.reconcile(purchaseId, invoiceId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('reconciliation:view')")
    public ReconciliationResultResponse getById(@PathVariable Long id) {
        return ReconciliationResultResponse.from(reconciliationService.getById(id));
    }

    @GetMapping("/purchase/{purchaseId}")
    @PreAuthorize("hasAuthority('reconciliation:view')")
    public ReconciliationResultResponse getForPurchase(@PathVariable Long purchaseId) {
        return ReconciliationResultResponse.from(reconciliationService.getForPurchase(purchaseId));
    }
}
