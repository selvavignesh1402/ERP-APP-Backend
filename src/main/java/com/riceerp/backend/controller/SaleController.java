package com.riceerp.backend.controller;

import com.riceerp.backend.dto.*;

import com.riceerp.backend.dto.ProductSalesHistoryResponse;
import com.riceerp.backend.dto.SaleResponse;
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
@RequestMapping({"/api/sales", "/sales"})
public class SaleController {

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('sale:create')")
    public SaleResponse createSale(@Valid @RequestBody SaleRequest request) {
        return SaleResponse.from(saleService.createSale(request));
    }

    @PostMapping("/sync")
    @PreAuthorize("hasAuthority('sale:create')")
    public com.riceerp.backend.dto.SyncBatchResponse syncSales(
            @RequestBody List<com.riceerp.backend.dto.OfflineSaleSyncRequest> requests) {
        return saleService.syncBatchSales(requests);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('sale:view')")
    public List<SaleResponse> listSales() {
        return saleService.listSales().stream().map(SaleResponse::from).toList();
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('sale:view')")
    public com.riceerp.backend.dto.SalesSummary salesSummary() {
        return saleService.salesSummary();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('sale:view')")
    public SaleResponse getSaleById(@PathVariable Long id) {
        return SaleResponse.from(saleService.getSaleById(id));
    }

    @GetMapping("/{id}/items")
    @PreAuthorize("hasAuthority('sale:view')")
    public List<SaleItemResponse> getSaleItems(@PathVariable Long id) {
        return saleService.getSaleItems(id).stream().map(SaleItemResponse::from).toList();
    }

    @GetMapping("/product/{productId}/history")
    @PreAuthorize("hasAuthority('sale:view') or hasAuthority('report:view')")
    public ProductSalesHistoryResponse getProductSalesHistory(
            @PathVariable Long productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return saleService.getProductSalesHistory(productId, start, end);
    }
}
