package com.riceerp.backend.controller;

import com.riceerp.backend.dto.ProductRequest;
import com.riceerp.backend.entity.PriceHistory;
import com.riceerp.backend.entity.Product;
import com.riceerp.backend.service.ProductService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public Product createProduct(@RequestBody ProductRequest request) {
        return productService.createProduct(request);
    }

    @PutMapping("/{id}")
    public Product updateProduct(@PathVariable Long id, @RequestBody ProductRequest request) {
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
}
