package com.riceerp.backend.controller;

import com.riceerp.backend.dto.*;

import com.riceerp.backend.dto.ProductRequest;
import com.riceerp.backend.dto.SupplierOptionResponse;
import com.riceerp.backend.entity.PriceHistory;
import com.riceerp.backend.entity.Product;
import com.riceerp.backend.service.ProductService;
import com.riceerp.backend.service.SupplierProductService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping({"/api/products", "/products"})
public class ProductController {

    private final ProductService productService;
    private final SupplierProductService supplierProductService;

    public ProductController(ProductService productService, SupplierProductService supplierProductService) {
        this.productService = productService;
        this.supplierProductService = supplierProductService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('product:create')")
    public ProductResponse createProduct(@Valid @RequestBody ProductRequest request) {
        return ProductResponse.from(productService.createProduct(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('product:edit')")
    public ProductResponse updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return ProductResponse.from(productService.updateProduct(id, request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('product:view') or hasAuthority('report:view')")
    public List<ProductResponse> listProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category) {
        return productService.listProducts(search, category).stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('product:view')")
    public ProductResponse getProductById(@PathVariable Long id) {
        return ProductResponse.from(productService.getProductById(id));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('product:status')")
    public ProductResponse toggleStatus(@PathVariable Long id, @RequestParam String status) {
        return ProductResponse.from(productService.toggleProductStatus(id, status));
    }

    @GetMapping("/{id}/price-history")
    @PreAuthorize("hasAuthority('product:view')")
    public List<PriceHistoryResponse> getPriceHistory(@PathVariable Long id) {
        return productService.getPriceHistory(id).stream().map(PriceHistoryResponse::from).toList();
    }

    @GetMapping("/{id}/suppliers")
    @PreAuthorize("hasAuthority('product:view')")
    public List<SupplierOptionResponse> getSupplierOptions(@PathVariable Long id) {
        return supplierProductService.getSupplierOptionsForProduct(id);
    }
}
