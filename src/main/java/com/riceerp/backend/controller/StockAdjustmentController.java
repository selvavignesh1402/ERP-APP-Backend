package com.riceerp.backend.controller;

import com.riceerp.backend.dto.StockAdjustmentRequest;
import com.riceerp.backend.entity.StockAdjustment;
import com.riceerp.backend.service.StockAdjustmentService;
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
    public StockAdjustment createAdjustment(@RequestBody StockAdjustmentRequest request) {
        return stockAdjustmentService.createAdjustment(request);
    }

    @GetMapping
    public List<StockAdjustment> listAdjustments(@RequestParam(required = false) Long productId) {
        return stockAdjustmentService.listAdjustmentsByProduct(productId);
    }
}
