package com.riceerp.backend.controller;

import com.riceerp.backend.dto.ProductSalesHistoryResponse;
import com.riceerp.backend.dto.SaleRequest;
import com.riceerp.backend.entity.Sale;
import com.riceerp.backend.entity.SaleItem;
import com.riceerp.backend.service.SaleService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/sales")
public class SaleController {

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('sale:create')")
    public Sale createSale(@Valid @RequestBody SaleRequest request) {
        return saleService.createSale(request);
    }

    @PostMapping("/sync")
    @PreAuthorize("hasAuthority('sale:create')")
    public com.riceerp.backend.dto.SyncBatchResponse syncSales(
            @RequestBody List<com.riceerp.backend.dto.OfflineSaleSyncRequest> requests) {
        return saleService.syncBatchSales(requests);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('sale:view')")
    public List<Sale> listSales() {
        return saleService.listSales();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('sale:view')")
    public Sale getSaleById(@PathVariable Long id) {
        return saleService.getSaleById(id);
    }

    @GetMapping("/{id}/items")
    @PreAuthorize("hasAuthority('sale:view')")
    public List<SaleItem> getSaleItems(@PathVariable Long id) {
        return saleService.getSaleItems(id);
    }

    @GetMapping("/product/{productId}/history")
    @PreAuthorize("hasAuthority('sale:view')")
    public ProductSalesHistoryResponse getProductSalesHistory(
            @PathVariable Long productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return saleService.getProductSalesHistory(productId, start, end);
    }
}
