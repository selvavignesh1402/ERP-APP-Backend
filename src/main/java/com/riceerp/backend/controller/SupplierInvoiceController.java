package com.riceerp.backend.controller;

import com.riceerp.backend.dto.SupplierInvoiceRequest;
import com.riceerp.backend.entity.SupplierInvoice;
import com.riceerp.backend.entity.SupplierInvoiceItem;
import com.riceerp.backend.service.SupplierInvoiceService;
import jakarta.validation.Valid;
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
    public SupplierInvoice createInvoice(@Valid @RequestBody SupplierInvoiceRequest request) {
        return invoiceService.createInvoice(request);
    }

    @GetMapping
    public List<SupplierInvoice> listInvoices(@RequestParam(required = false) Long supplierId) {
        return invoiceService.listInvoices(supplierId);
    }

    @GetMapping("/{id}")
    public SupplierInvoice getInvoice(@PathVariable Long id) {
        return invoiceService.getInvoiceById(id);
    }

    @GetMapping("/{id}/items")
    public List<SupplierInvoiceItem> getInvoiceItems(@PathVariable Long id) {
        return invoiceService.getInvoiceItems(id);
    }

    @PutMapping("/{id}/status")
    public SupplierInvoice updateStatus(@PathVariable Long id, @RequestParam String status) {
        return invoiceService.updateStatus(id, status);
    }

    @GetMapping("/purchase/{purchaseId}")
    public List<SupplierInvoice> getInvoicesForPurchase(@PathVariable Long purchaseId) {
        return invoiceService.getInvoicesForPurchase(purchaseId);
    }

    @GetMapping("/purchase/{purchaseId}/items")
    public List<SupplierInvoiceItem> getInvoiceItemsForPurchase(@PathVariable Long purchaseId) {
        return invoiceService.getInvoiceItemsByPurchase(purchaseId);
    }
}