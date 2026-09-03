package com.riceerp.backend.controller;

import com.riceerp.backend.dto.SupplierOptionResponse;
import com.riceerp.backend.dto.SupplierProductRequest;
import com.riceerp.backend.entity.SupplierProduct;
import com.riceerp.backend.service.SupplierProductService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/suppliers")
public class SupplierProductController {

    private final SupplierProductService supplierProductService;

    public SupplierProductController(SupplierProductService supplierProductService) {
        this.supplierProductService = supplierProductService;
    }

    @PostMapping("/{id}/products")
    public SupplierProduct assignProduct(@PathVariable Long id, @Valid @RequestBody SupplierProductRequest request) {
        return supplierProductService.assignProduct(id, request);
    }

    @PutMapping("/{supplierId}/products/{productId}")
    public SupplierProduct updateProcurementData(@PathVariable Long supplierId,
                                                 @PathVariable Long productId,
                                                 @Valid @RequestBody SupplierProductRequest request) {
        return supplierProductService.updateProcurementData(supplierId, productId, request);
    }

    @GetMapping("/{id}/products")
    public List<SupplierProduct> listSupplierProducts(@PathVariable Long id) {
        return supplierProductService.listSupplierProducts(id);
    }
}