package com.riceerp.backend.controller;

import com.riceerp.backend.dto.StockAdjustmentRequest;
import com.riceerp.backend.entity.StockAdjustment;
import com.riceerp.backend.service.StockAdjustmentService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory/adjustments")
public class StockAdjustmentController {

    private final StockAdjustmentService stockAdjustmentService;

    public StockAdjustmentController(StockAdjustmentService stockAdjustmentService) {
        this.stockAdjustmentService = stockAdjustmentService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('stock:adjust')")
    public StockAdjustment createAdjustment(@Valid @RequestBody StockAdjustmentRequest request) {
        return stockAdjustmentService.createAdjustment(request);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('inventory:view')")
    public List<StockAdjustment> listAdjustments(@RequestParam(required = false) Long productId) {
        return stockAdjustmentService.listAdjustmentsByProduct(productId);
    }
}
