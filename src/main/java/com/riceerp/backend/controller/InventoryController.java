package com.riceerp.backend.controller;

import com.riceerp.backend.entity.Product;
import com.riceerp.backend.entity.StockMovement;
import com.riceerp.backend.enums.Status;
import com.riceerp.backend.repository.ProductRepository;
import com.riceerp.backend.service.StockMovementService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final ProductRepository productRepository;
    private final StockMovementService stockMovementService;

    public InventoryController(ProductRepository productRepository, StockMovementService stockMovementService) {
        this.productRepository = productRepository;
        this.stockMovementService = stockMovementService;
    }

    @GetMapping("/low-stock")
    public List<Map<String, Object>> lowStock() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Product p : productRepository.findByStatus(Status.ACTIVE)) {
            if (p.getStock() < p.getMinimumStock()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("productId", p.getId());
                row.put("productName", p.getProductName());
                row.put("unit", p.getUnit());
                row.put("currentStock", p.getStock());
                row.put("reorderLevel", p.getMinimumStock());
                row.put("shortageAmount", Math.round((p.getMinimumStock() - p.getStock()) * 100.0) / 100.0);
                result.add(row);
            }
        }
        return result;
    }

    @GetMapping("/movements")
    public List<StockMovement> movements(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return stockMovementService.listMovements(productId, start, end);
    }
}