package com.riceerp.backend.controller;

import com.riceerp.backend.dto.*;

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
@RequestMapping({"/api/purchases", "/purchases"})
public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('purchase:create')")
    public PurchaseResponse createPurchase(@Valid @RequestBody PurchaseRequest request) {
        return PurchaseResponse.from(purchaseService.createPurchase(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('purchase:view')")
    public List<PurchaseResponse> listPurchases(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String invoiceNumber) {
        return purchaseService.listPurchases(supplierId, invoiceNumber).stream().map(PurchaseResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('purchase:view')")
    public PurchaseResponse getPurchaseById(@PathVariable Long id) {
        return PurchaseResponse.from(purchaseService.getPurchaseById(id));
    }

    @GetMapping("/{id}/items")
    @PreAuthorize("hasAuthority('purchase:view')")
    public List<PurchaseItemResponse> getPurchaseItems(@PathVariable Long id) {
        return purchaseService.getPurchaseItems(id).stream().map(PurchaseItemResponse::from).toList();
    }

    @PutMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('purchase:create')")
    public PurchaseResponse submitPurchase(@PathVariable Long id) {
        return PurchaseResponse.from(purchaseService.submit(id));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('purchase:approve')")
    public PurchaseResponse approvePurchase(@PathVariable Long id) {
        return PurchaseResponse.from(purchaseService.approve(id));
    }

    @PutMapping("/{id}/order")
    @PreAuthorize("hasAuthority('purchase:approve')")
    public PurchaseResponse orderPurchase(@PathVariable Long id) {
        return PurchaseResponse.from(purchaseService.order(id));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('purchase:approve')")
    public PurchaseResponse cancelPurchase(@PathVariable Long id) {
        return PurchaseResponse.from(purchaseService.cancel(id));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('purchase:approve')")
    public PurchaseResponse updatePurchaseStatus(@PathVariable Long id,
                                         @RequestBody PurchaseStatusUpdateRequest request) {
        return PurchaseResponse.from(purchaseService.updateStatus(id, request.getStatus()));
    }

    @PostMapping("/{id}/returns")
    @PreAuthorize("hasAuthority('purchase:create')")
    public PurchaseReturnResponse createPurchaseReturn(@PathVariable Long id,
                                               @Valid @RequestBody PurchaseReturnRequest request) {
        return PurchaseReturnResponse.from(purchaseService.createPurchaseReturn(id, request));
    }
}
