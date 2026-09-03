package com.riceerp.backend.service;

import com.riceerp.backend.dto.ProductRequest;
import com.riceerp.backend.entity.PriceHistory;
import com.riceerp.backend.entity.Product;
import com.riceerp.backend.enums.PriceType;
import com.riceerp.backend.enums.Status;
import com.riceerp.backend.exception.NotFoundException;
import com.riceerp.backend.repository.PriceHistoryRepository;
import com.riceerp.backend.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    public ProductService(ProductRepository productRepository, PriceHistoryRepository priceHistoryRepository) {
        this.productRepository = productRepository;
        this.priceHistoryRepository = priceHistoryRepository;
    }

    @Transactional
    public Product createProduct(ProductRequest request) {
        Product product = new Product();
        product.setProductName(request.getProductName());
        product.setCategory(request.getCategory());
        product.setBrand(request.getBrand());
        product.setUnit(request.getUnit());
        product.setPurchasePrice(request.getPurchasePrice());
        product.setSellingPrice(request.getSellingPrice());
        product.setStock(request.getStock());
        product.setMinimumStock(request.getMinimumStock());
        product.setGstRate(request.getGstRate());
        product.setHsnCode(request.getHsnCode());
        product.setStatus(Status.ACTIVE);

        Product savedProduct = productRepository.save(product);

        // Record initial price history
        priceHistoryRepository.save(new PriceHistory(savedProduct, PriceType.PURCHASE, request.getPurchasePrice()));
        priceHistoryRepository.save(new PriceHistory(savedProduct, PriceType.SELLING, request.getSellingPrice()));

        return savedProduct;
    }

    @Transactional
    public Product updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + id));

        // Check for purchase price change
        if (product.getPurchasePrice() != request.getPurchasePrice()) {
            product.setPurchasePrice(request.getPurchasePrice());
            priceHistoryRepository.save(new PriceHistory(product, PriceType.PURCHASE, request.getPurchasePrice()));
        }

        // Check for selling price change
        if (product.getSellingPrice() != request.getSellingPrice()) {
            product.setSellingPrice(request.getSellingPrice());
            priceHistoryRepository.save(new PriceHistory(product, PriceType.SELLING, request.getSellingPrice()));
        }

        product.setProductName(request.getProductName());
        product.setCategory(request.getCategory());
        product.setBrand(request.getBrand());
        product.setUnit(request.getUnit());
        product.setStock(request.getStock());
        product.setMinimumStock(request.getMinimumStock());
        product.setGstRate(request.getGstRate());
        product.setHsnCode(request.getHsnCode());

        return productRepository.save(product);
    }

    public List<Product> listProducts(String search, String category) {
        if (search != null && !search.trim().isEmpty()) {
            return productRepository.findByProductNameContainingIgnoreCase(search);
        }
        if (category != null && !category.trim().isEmpty()) {
            return productRepository.findByCategoryIgnoreCase(category);
        }
        return productRepository.findAll();
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + id));
    }

    @Transactional
    public Product toggleProductStatus(Long id, String status) {
        Product product = getProductById(id);
        product.setStatus(Status.valueOf(status.toUpperCase()));
        return productRepository.save(product);
    }

    public List<PriceHistory> getPriceHistory(Long productId) {
        // Ensure product exists
        getProductById(productId);
        return priceHistoryRepository.findByProductIdOrderByEffectiveFromDesc(productId);
    }
}
