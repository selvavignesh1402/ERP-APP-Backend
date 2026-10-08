package com.riceerp.backend.controller;

import com.riceerp.backend.dto.*;

import com.riceerp.backend.dto.SalesOrderRequest;
import com.riceerp.backend.entity.SalesOrder;
import com.riceerp.backend.enums.SalesOrderStatus;
import com.riceerp.backend.service.SalesOrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sales-orders")
public class SalesOrderController {

    private final SalesOrderService salesOrderService;

    public SalesOrderController(SalesOrderService salesOrderService) {
        this.salesOrderService = salesOrderService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('sales-order:create')")
    public ResponseEntity<SalesOrderResponse> createSalesOrder(@Valid @RequestBody SalesOrderRequest request, Authentication authentication) {
        if (request.getSalespersonId() == null && authentication != null) {
            try {
                Long userId = Long.parseLong(authentication.getPrincipal().toString());
                request.setSalespersonId(userId);
            } catch (Exception ignored) {}
        }
        SalesOrder order = salesOrderService.createSalesOrder(request);
        return ResponseEntity.ok(SalesOrderResponse.from(order));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('sales-order:view')")
    public List<SalesOrderResponse> listSalesOrders(@RequestParam(required = false) SalesOrderStatus status,
                                            @RequestParam(required = false) Long customerId,
                                            @RequestParam(required = false) Long salespersonId) {
        return salesOrderService.listSalesOrders(status, customerId, salespersonId).stream().map(SalesOrderResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('sales-order:view')")
    public SalesOrderResponse getSalesOrderById(@PathVariable Long id) {
        return SalesOrderResponse.from(salesOrderService.getSalesOrderById(id));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('delivery:create')")
    public SalesOrderResponse updateStatus(@PathVariable Long id, @RequestParam SalesOrderStatus status) {
        return SalesOrderResponse.from(salesOrderService.updateStatus(id, status));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('sales-order:cancel')")
    public SalesOrderResponse cancelSalesOrder(@PathVariable Long id, @RequestBody(required = false) Map<String, String> request) {
        String reason = request != null ? request.get("reason") : null;
        return SalesOrderResponse.from(salesOrderService.cancelSalesOrder(id, reason));
    }

    @GetMapping("/{id}/stock-check")
    @PreAuthorize("hasAuthority('sales-order:view')")
    public Map<String, Object> checkStockAvailability(@PathVariable Long id) {
        return salesOrderService.checkStockAvailability(id);
    }

    @GetMapping("/fulfillment-counts")
    @PreAuthorize("hasAuthority('sales-order:view')")
    public Map<String, Long> getFulfillmentCounts() {
        return salesOrderService.getFulfillmentCounts();
    }
}
