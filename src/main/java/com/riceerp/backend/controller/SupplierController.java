package com.riceerp.backend.controller;

import com.riceerp.backend.dto.SupplierRequest;
import com.riceerp.backend.entity.Supplier;
import com.riceerp.backend.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('supplier:create')")
    public Supplier createSupplier(@Valid @RequestBody SupplierRequest request) {
        return supplierService.createSupplier(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('supplier:edit')")
    public Supplier updateSupplier(@PathVariable Long id, @Valid @RequestBody SupplierRequest request) {
        return supplierService.updateSupplier(id, request);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('supplier:view')")
    public List<Supplier> listSuppliers(@RequestParam(required = false) String search) {
        return supplierService.listSuppliers(search);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('supplier:view')")
    public Supplier getSupplierById(@PathVariable Long id) {
        return supplierService.getSupplierById(id);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('supplier:status')")
    public Supplier toggleStatus(@PathVariable Long id, @RequestParam String status) {
        return supplierService.toggleSupplierStatus(id, status);
    }
}
