package com.riceerp.backend.controller;

import com.riceerp.backend.dto.SupplierRequest;
import com.riceerp.backend.entity.Supplier;
import com.riceerp.backend.service.SupplierService;
import jakarta.validation.Valid;
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
    public Supplier createSupplier(@Valid @RequestBody SupplierRequest request) {
        return supplierService.createSupplier(request);
    }

    @PutMapping("/{id}")
    public Supplier updateSupplier(@PathVariable Long id, @Valid @RequestBody SupplierRequest request) {
        return supplierService.updateSupplier(id, request);
    }

    @GetMapping
    public List<Supplier> listSuppliers(@RequestParam(required = false) String search) {
        return supplierService.listSuppliers(search);
    }

    @GetMapping("/{id}")
    public Supplier getSupplierById(@PathVariable Long id) {
        return supplierService.getSupplierById(id);
    }

    @PutMapping("/{id}/status")
    public Supplier toggleStatus(@PathVariable Long id, @RequestParam String status) {
        return supplierService.toggleSupplierStatus(id, status);
    }
}
