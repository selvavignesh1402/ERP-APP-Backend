package com.riceerp.backend.controller;

import com.riceerp.backend.dto.ProductRequest;
import com.riceerp.backend.dto.SupplierOptionResponse;
import com.riceerp.backend.entity.PriceHistory;
import com.riceerp.backend.entity.Product;
import com.riceerp.backend.service.ProductService;
import com.riceerp.backend.service.SupplierProductService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final SupplierProductService supplierProductService;

    public ProductController(ProductService productService, SupplierProductService supplierProductService) {
        this.productService = productService;
        this.supplierProductService = supplierProductService;
    }

    @PostMapping
    public Product createProduct(@Valid @RequestBody ProductRequest request) {
        return productService.createProduct(request);
    }

    @PutMapping("/{id}")
    public Product updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.updateProduct(id, request);
    }

    @GetMapping
    public List<Product> listProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category) {
        return productService.listProducts(search, category);
    }

    @GetMapping("/{id}")
    public Product getProductById(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    @PutMapping("/{id}/status")
    public Product toggleStatus(@PathVariable Long id, @RequestParam String status) {
        return productService.toggleProductStatus(id, status);
    }

    @GetMapping("/{id}/price-history")
    public List<PriceHistory> getPriceHistory(@PathVariable Long id) {
        return productService.getPriceHistory(id);
    }

    @GetMapping("/{id}/suppliers")
    public List<SupplierOptionResponse> getSupplierOptions(@PathVariable Long id) {
        return supplierProductService.getSupplierOptionsForProduct(id);
    }
}
