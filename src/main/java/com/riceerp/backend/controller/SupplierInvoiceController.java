package com.riceerp.backend.controller;

import com.riceerp.backend.dto.SupplierInvoiceRequest;
import com.riceerp.backend.entity.SupplierInvoice;
import com.riceerp.backend.entity.SupplierInvoiceItem;
import com.riceerp.backend.service.SupplierInvoiceService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/invoices")
public class SupplierInvoiceController {

    private final SupplierInvoiceService invoiceService;

    public SupplierInvoiceController(SupplierInvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('purchase:create') or hasAuthority('invoice:view')")
    public SupplierInvoice createInvoice(@Valid @RequestBody SupplierInvoiceRequest request) {
        return invoiceService.createInvoice(request);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('invoice:view')")
    public List<SupplierInvoice> listInvoices(@RequestParam(required = false) Long supplierId) {
        return invoiceService.listInvoices(supplierId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('invoice:view')")
    public SupplierInvoice getInvoice(@PathVariable Long id) {
        return invoiceService.getInvoiceById(id);
    }

    @GetMapping("/{id}/items")
    @PreAuthorize("hasAuthority('invoice:view')")
    public List<SupplierInvoiceItem> getInvoiceItems(@PathVariable Long id) {
        return invoiceService.getInvoiceItems(id);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('invoice:status')")
    public SupplierInvoice updateStatus(@PathVariable Long id, @RequestParam String status) {
        return invoiceService.updateStatus(id, status);
    }

    @GetMapping("/purchase/{purchaseId}")
    @PreAuthorize("hasAuthority('invoice:view')")
    public List<SupplierInvoice> getInvoicesForPurchase(@PathVariable Long purchaseId) {
        return invoiceService.getInvoicesForPurchase(purchaseId);
    }

    @GetMapping("/purchase/{purchaseId}/items")
    @PreAuthorize("hasAuthority('invoice:view')")
    public List<SupplierInvoiceItem> getInvoiceItemsForPurchase(@PathVariable Long purchaseId) {
        return invoiceService.getInvoiceItemsByPurchase(purchaseId);
    }
}