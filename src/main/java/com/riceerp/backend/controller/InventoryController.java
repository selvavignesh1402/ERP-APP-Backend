package com.riceerp.backend.controller;

import com.riceerp.backend.dto.*;

import com.riceerp.backend.entity.Product;
import com.riceerp.backend.entity.StockMovement;
import com.riceerp.backend.enums.Status;
import com.riceerp.backend.repository.ProductRepository;
import com.riceerp.backend.service.StockMovementService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/inventory", "/inventory"})
public class InventoryController {

    private final ProductRepository productRepository;
    private final StockMovementService stockMovementService;

    public InventoryController(ProductRepository productRepository, StockMovementService stockMovementService) {
        this.productRepository = productRepository;
        this.stockMovementService = stockMovementService;
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAuthority('inventory:view') or hasAuthority('report:view')")
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
    @PreAuthorize("hasAuthority('inventory:view')")
    public List<StockMovementResponse> movements(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return stockMovementService.listMovements(productId, start, end).stream().map(StockMovementResponse::from).toList();
    }

    @GetMapping("/movements/page")
    @PreAuthorize("hasAuthority('inventory:view') or hasAuthority('report:view')")
    public MovementPageResponse movementPage(@RequestParam(required = false) Long beforeId,
                                                          @RequestParam(defaultValue = "50") int size) {
        var page = stockMovementService.movementPage(beforeId, size);
        return new MovementPageResponse(page.items().stream().map(StockMovementResponse::from).toList(), page.nextBeforeId());
    }
    public record MovementPageResponse(List<StockMovementResponse> items, Long nextBeforeId) { }
}
