package com.riceerp.backend.controller;

import com.riceerp.backend.dto.GoodsReceiptRequest;
import com.riceerp.backend.entity.GoodsReceipt;
import com.riceerp.backend.entity.GoodsReceiptItem;
import com.riceerp.backend.service.GoodsReceiptService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/purchases")
public class GoodsReceiptController {

    private final GoodsReceiptService goodsReceiptService;

    public GoodsReceiptController(GoodsReceiptService goodsReceiptService) {
        this.goodsReceiptService = goodsReceiptService;
    }

    @PostMapping("/{id}/receipts")
    @PreAuthorize("hasAuthority('purchase:create')")
    public GoodsReceipt createReceipt(@PathVariable Long id, @Valid @RequestBody GoodsReceiptRequest request) {
        return goodsReceiptService.createReceipt(id, request);
    }

    @GetMapping("/{id}/receipts")
    @PreAuthorize("hasAuthority('purchase:view')")
    public List<GoodsReceipt> listReceipts(@PathVariable Long id) {
        return goodsReceiptService.listReceiptsForPurchase(id);
    }

    @GetMapping("/{id}/receipts/{receiptId}")
    @PreAuthorize("hasAuthority('purchase:view')")
    public GoodsReceipt getReceipt(@PathVariable Long id, @PathVariable Long receiptId) {
        return goodsReceiptService.getReceiptById(receiptId);
    }

    @GetMapping("/{id}/receipts/{receiptId}/items")
    @PreAuthorize("hasAuthority('purchase:view')")
    public List<GoodsReceiptItem> getReceiptItems(@PathVariable Long id, @PathVariable Long receiptId) {
        return goodsReceiptService.getReceiptItems(receiptId);
    }
}