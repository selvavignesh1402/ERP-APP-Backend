package com.riceerp.backend.controller;

import com.riceerp.backend.dto.*;

import com.riceerp.backend.dto.SupplierInvoiceRequest;
import com.riceerp.backend.entity.SupplierInvoice;
import com.riceerp.backend.entity.SupplierInvoiceItem;
import com.riceerp.backend.service.SupplierInvoiceService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/invoices", "/invoices"})
public class SupplierInvoiceController {

    private final SupplierInvoiceService invoiceService;

    public SupplierInvoiceController(SupplierInvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('purchase:create') or hasAuthority('invoice:view')")
    public SupplierInvoiceResponse createInvoice(@Valid @RequestBody SupplierInvoiceRequest request) {
        return SupplierInvoiceResponse.from(invoiceService.createInvoice(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('invoice:view')")
    public List<SupplierInvoiceResponse> listInvoices(@RequestParam(required = false) Long supplierId) {
        return invoiceService.listInvoices(supplierId).stream().map(SupplierInvoiceResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('invoice:view')")
    public SupplierInvoiceResponse getInvoice(@PathVariable Long id) {
        return SupplierInvoiceResponse.from(invoiceService.getInvoiceById(id));
    }

    @GetMapping("/{id}/items")
    @PreAuthorize("hasAuthority('invoice:view')")
    public List<SupplierInvoiceItemResponse> getInvoiceItems(@PathVariable Long id) {
        return invoiceService.getInvoiceItems(id).stream().map(SupplierInvoiceItemResponse::from).toList();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('invoice:status')")
    public SupplierInvoiceResponse updateStatus(@PathVariable Long id, @RequestParam String status) {
        return SupplierInvoiceResponse.from(invoiceService.updateStatus(id, status));
    }

    @GetMapping("/purchase/{purchaseId}")
    @PreAuthorize("hasAuthority('invoice:view')")
    public List<SupplierInvoiceResponse> getInvoicesForPurchase(@PathVariable Long purchaseId) {
        return invoiceService.getInvoicesForPurchase(purchaseId).stream().map(SupplierInvoiceResponse::from).toList();
    }

    @GetMapping("/purchase/{purchaseId}/items")
    @PreAuthorize("hasAuthority('invoice:view')")
    public List<SupplierInvoiceItemResponse> getInvoiceItemsForPurchase(@PathVariable Long purchaseId) {
        return invoiceService.getInvoiceItemsByPurchase(purchaseId).stream().map(SupplierInvoiceItemResponse::from).toList();
    }
}
