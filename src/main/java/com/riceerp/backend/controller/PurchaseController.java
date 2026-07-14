package com.riceerp.backend.controller;

import com.riceerp.backend.dto.PurchaseRequest;
import com.riceerp.backend.dto.PurchaseReturnRequest;
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
