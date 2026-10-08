package com.riceerp.backend.controller;

import com.riceerp.backend.dto.*;

import com.riceerp.backend.dto.GoodsReceiptRequest;
import com.riceerp.backend.entity.GoodsReceipt;
import com.riceerp.backend.entity.GoodsReceiptItem;
import com.riceerp.backend.service.GoodsReceiptService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/purchases", "/purchases"})
public class GoodsReceiptController {

    private final GoodsReceiptService goodsReceiptService;

    public GoodsReceiptController(GoodsReceiptService goodsReceiptService) {
        this.goodsReceiptService = goodsReceiptService;
    }

    @PostMapping("/{id}/receipts")
    @PreAuthorize("hasAuthority('purchase:create')")
    public GoodsReceiptResponse createReceipt(@PathVariable Long id, @Valid @RequestBody GoodsReceiptRequest request) {
        return GoodsReceiptResponse.from(goodsReceiptService.createReceipt(id, request));
    }

    @GetMapping("/{id}/receipts")
    @PreAuthorize("hasAuthority('purchase:view')")
    public List<GoodsReceiptResponse> listReceipts(@PathVariable Long id) {
        return goodsReceiptService.listReceiptsForPurchase(id).stream().map(GoodsReceiptResponse::from).toList();
    }

    @GetMapping("/{id}/receipts/{receiptId}")
    @PreAuthorize("hasAuthority('purchase:view')")
    public GoodsReceiptResponse getReceipt(@PathVariable Long id, @PathVariable Long receiptId) {
        return GoodsReceiptResponse.from(goodsReceiptService.getReceiptById(receiptId));
    }

    @GetMapping("/{id}/receipts/{receiptId}/items")
    @PreAuthorize("hasAuthority('purchase:view')")
    public List<GoodsReceiptItemResponse> getReceiptItems(@PathVariable Long id, @PathVariable Long receiptId) {
        return goodsReceiptService.getReceiptItems(receiptId).stream().map(GoodsReceiptItemResponse::from).toList();
    }
}
