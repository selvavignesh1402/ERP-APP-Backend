package com.riceerp.backend.controller;

import com.riceerp.backend.dto.*;

import com.riceerp.backend.dto.SupplierRequest;
import com.riceerp.backend.entity.Supplier;
import com.riceerp.backend.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/suppliers", "/suppliers"})
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('supplier:create')")
    public SupplierResponse createSupplier(@Valid @RequestBody SupplierRequest request) {
        return SupplierResponse.from(supplierService.createSupplier(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('supplier:edit')")
    public SupplierResponse updateSupplier(@PathVariable Long id, @Valid @RequestBody SupplierRequest request) {
        return SupplierResponse.from(supplierService.updateSupplier(id, request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('supplier:view')")
    public List<SupplierResponse> listSuppliers(@RequestParam(required = false) String search) {
        return supplierService.listSuppliers(search).stream().map(SupplierResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('supplier:view')")
    public SupplierResponse getSupplierById(@PathVariable Long id) {
        return SupplierResponse.from(supplierService.getSupplierById(id));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('supplier:status')")
    public SupplierResponse toggleStatus(@PathVariable Long id, @RequestParam String status) {
        return SupplierResponse.from(supplierService.toggleSupplierStatus(id, status));
    }
}
