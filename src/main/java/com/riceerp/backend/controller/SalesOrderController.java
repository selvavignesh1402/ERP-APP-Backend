package com.riceerp.backend.controller;

import com.riceerp.backend.dto.SalesOrderRequest;
import com.riceerp.backend.entity.SalesOrder;
import com.riceerp.backend.enums.SalesOrderStatus;
import com.riceerp.backend.service.SalesOrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<SalesOrder> createSalesOrder(@Valid @RequestBody SalesOrderRequest request, Authentication authentication) {
        if (request.getSalespersonId() == null && authentication != null) {
            try {
                Long userId = Long.parseLong(authentication.getPrincipal().toString());
                request.setSalespersonId(userId);
            } catch (Exception ignored) {}
        }
        SalesOrder order = salesOrderService.createSalesOrder(request);
        return ResponseEntity.ok(order);
    }

    @GetMapping
    public List<SalesOrder> listSalesOrders(@RequestParam(required = false) SalesOrderStatus status,
                                            @RequestParam(required = false) Long customerId,
                                            @RequestParam(required = false) Long salespersonId) {
        return salesOrderService.listSalesOrders(status, customerId, salespersonId);
    }

    @GetMapping("/{id}")
    public SalesOrder getSalesOrderById(@PathVariable Long id) {
        return salesOrderService.getSalesOrderById(id);
    }

    @PutMapping("/{id}/status")
    public SalesOrder updateStatus(@PathVariable Long id, @RequestParam SalesOrderStatus status) {
        return salesOrderService.updateStatus(id, status);
    }

    @PutMapping("/{id}/cancel")
    public SalesOrder cancelSalesOrder(@PathVariable Long id, @RequestBody(required = false) Map<String, String> request) {
        String reason = request != null ? request.get("reason") : null;
        return salesOrderService.cancelSalesOrder(id, reason);
    }

    @GetMapping("/{id}/stock-check")
    public Map<String, Object> checkStockAvailability(@PathVariable Long id) {
        return salesOrderService.checkStockAvailability(id);
    }

    @GetMapping("/fulfillment-counts")
    public Map<String, Long> getFulfillmentCounts() {
        return salesOrderService.getFulfillmentCounts();
    }
}
