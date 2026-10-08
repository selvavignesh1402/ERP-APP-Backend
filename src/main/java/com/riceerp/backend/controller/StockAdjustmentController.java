package com.riceerp.backend.controller;

import com.riceerp.backend.dto.*;

import com.riceerp.backend.dto.StockAdjustmentRequest;
import com.riceerp.backend.entity.StockAdjustment;
import com.riceerp.backend.service.StockAdjustmentService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/stock-adjustments", "/inventory/adjustments"})
public class StockAdjustmentController {

    private final StockAdjustmentService stockAdjustmentService;

    public StockAdjustmentController(StockAdjustmentService stockAdjustmentService) {
        this.stockAdjustmentService = stockAdjustmentService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('stock:adjust')")
    public StockAdjustmentResponse createAdjustment(@Valid @RequestBody StockAdjustmentRequest request) {
        return StockAdjustmentResponse.from(stockAdjustmentService.createAdjustment(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('inventory:view')")
    public List<StockAdjustmentResponse> listAdjustments(@RequestParam(required = false) Long productId) {
        return stockAdjustmentService.listAdjustmentsByProduct(productId).stream().map(StockAdjustmentResponse::from).toList();
    }
}
