package com.riceerp.backend.controller;

import com.riceerp.backend.dto.PurchaseRequest;
import com.riceerp.backend.dto.PurchaseReturnRequest;
import com.riceerp.backend.dto.PurchaseStatusUpdateRequest;
import com.riceerp.backend.entity.Purchase;
import com.riceerp.backend.entity.PurchaseItem;
import com.riceerp.backend.entity.PurchaseReturn;
import com.riceerp.backend.service.PurchaseService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/purchases")
public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('purchase:create')")
    public Purchase createPurchase(@Valid @RequestBody PurchaseRequest request) {
        return purchaseService.createPurchase(request);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('purchase:view')")
    public List<Purchase> listPurchases(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String invoiceNumber) {
        return purchaseService.listPurchases(supplierId, invoiceNumber);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('purchase:view')")
    public Purchase getPurchaseById(@PathVariable Long id) {
        return purchaseService.getPurchaseById(id);
    }

    @GetMapping("/{id}/items")
    @PreAuthorize("hasAuthority('purchase:view')")
    public List<PurchaseItem> getPurchaseItems(@PathVariable Long id) {
        return purchaseService.getPurchaseItems(id);
    }

    @PutMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('purchase:create')")
    public Purchase submitPurchase(@PathVariable Long id) {
        return purchaseService.submit(id);
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('purchase:approve')")
    public Purchase approvePurchase(@PathVariable Long id) {
        return purchaseService.approve(id);
    }

    @PutMapping("/{id}/order")
    @PreAuthorize("hasAuthority('purchase:approve')")
    public Purchase orderPurchase(@PathVariable Long id) {
        return purchaseService.order(id);
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('purchase:approve')")
    public Purchase cancelPurchase(@PathVariable Long id) {
        return purchaseService.cancel(id);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('purchase:approve')")
    public Purchase updatePurchaseStatus(@PathVariable Long id,
                                         @RequestBody PurchaseStatusUpdateRequest request) {
        return purchaseService.updateStatus(id, request.getStatus());
    }

    @PostMapping("/{id}/returns")
    @PreAuthorize("hasAuthority('purchase:create')")
    public PurchaseReturn createPurchaseReturn(@PathVariable Long id,
                                               @Valid @RequestBody PurchaseReturnRequest request) {
        return purchaseService.createPurchaseReturn(id, request);
    }
}