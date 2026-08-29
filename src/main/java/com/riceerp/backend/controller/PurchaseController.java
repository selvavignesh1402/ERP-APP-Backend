package com.riceerp.backend.controller;

import com.riceerp.backend.dto.PurchaseRequest;
import com.riceerp.backend.dto.PurchaseReturnRequest;
import com.riceerp.backend.dto.PurchaseStatusUpdateRequest;
import com.riceerp.backend.entity.Purchase;
import com.riceerp.backend.entity.PurchaseItem;
import com.riceerp.backend.entity.PurchaseReturn;
import com.riceerp.backend.service.PurchaseService;
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
    public Purchase createPurchase(@RequestBody PurchaseRequest request) {
        return purchaseService.createPurchase(request);
    }

    @GetMapping
    public List<Purchase> listPurchases(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String invoiceNumber) {
        return purchaseService.listPurchases(supplierId, invoiceNumber);
    }

    @GetMapping("/{id}")
    public Purchase getPurchaseById(@PathVariable Long id) {
        return purchaseService.getPurchaseById(id);
    }

    @GetMapping("/{id}/items")
    public List<PurchaseItem> getPurchaseItems(@PathVariable Long id) {
        return purchaseService.getPurchaseItems(id);
    }

    @PutMapping("/{id}/submit")
    public Purchase submitPurchase(@PathVariable Long id) {
        return purchaseService.submit(id);
    }

    @PutMapping("/{id}/approve")
    public Purchase approvePurchase(@PathVariable Long id) {
        return purchaseService.approve(id);
    }

    @PutMapping("/{id}/order")
    public Purchase orderPurchase(@PathVariable Long id) {
        return purchaseService.order(id);
    }

    @PutMapping("/{id}/cancel")
    public Purchase cancelPurchase(@PathVariable Long id) {
        return purchaseService.cancel(id);
    }

    @PutMapping("/{id}/status")
    public Purchase updatePurchaseStatus(@PathVariable Long id,
                                         @RequestBody PurchaseStatusUpdateRequest request) {
        return purchaseService.updateStatus(id, request.getStatus());
    }

    @PostMapping("/{id}/returns")
    public PurchaseReturn createPurchaseReturn(
            @PathVariable Long id,
            @RequestBody PurchaseReturnRequest request) {
        return purchaseService.createPurchaseReturn(id, request);
    }

    @GetMapping("/{id}/returns")
    public List<PurchaseReturn> getPurchaseReturns(@PathVariable Long id) {
        return purchaseService.getPurchaseReturns(id);
    }
}