package com.riceerp.backend.service;

import com.riceerp.backend.dto.ProductRequest;
import com.riceerp.backend.entity.PriceHistory;
import com.riceerp.backend.entity.Product;
import com.riceerp.backend.enums.PriceType;
import com.riceerp.backend.enums.Status;
import com.riceerp.backend.enums.MovementType;
import com.riceerp.backend.exception.NotFoundException;
import com.riceerp.backend.repository.PriceHistoryRepository;
import com.riceerp.backend.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {
    private static void validatePrices(ProductRequest request) {
        var purchase = request.getPurchasePrice();
        var selling = request.getSellingPrice();
        var maximum = new java.math.BigDecimal("999999999999999.9999");
        if (purchase == null || selling == null || purchase.signum() < 0 || selling.signum() <= 0 ||
                purchase.compareTo(maximum) > 0 || selling.compareTo(maximum) > 0 ||
                purchase.stripTrailingZeros().scale() > 4 || selling.stripTrailingZeros().scale() > 4)
            throw new com.riceerp.backend.exception.BusinessRuleException("Product prices must be within the supported range with at most four decimal places; selling price must be positive and purchase price non-negative.");
    }

    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final StockMovementService stockMovementService;

    public ProductService(ProductRepository productRepository, PriceHistoryRepository priceHistoryRepository,
                          StockMovementService stockMovementService) {
        this.productRepository = productRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.stockMovementService = stockMovementService;
    }

    @Transactional
    public Product createProduct(ProductRequest request) {
        validatePrices(request);
        PurchaseQuantities.nonNegative(request.getStock());
        GstCalculator.rate(request.getGstRate());
        Product product = new Product();
        product.setProductName(request.getProductName());
        product.setCategory(request.getCategory());
        product.setBrand(request.getBrand());
        product.setUnit(request.getUnit());
        product.setPurchasePrice(request.getPurchasePrice());
        product.setSellingPrice(request.getSellingPrice());
        product.setStock(request.getStock());
        product.setMinimumStock(request.getMinimumStock());
        product.setGstRate(GstCalculator.rate(request.getGstRate()));
        product.setHsnCode(request.getHsnCode());
        product.setStatus(Status.ACTIVE);

        Product savedProduct = productRepository.save(product);
        if (savedProduct.getStock() > 0) {
            stockMovementService.record(savedProduct, MovementType.OPENING_STOCK, savedProduct.getStock(), savedProduct.getId());
        }

        // Record initial price history
        priceHistoryRepository.save(new PriceHistory(savedProduct, PriceType.PURCHASE, request.getPurchasePrice()));
        priceHistoryRepository.save(new PriceHistory(savedProduct, PriceType.SELLING, request.getSellingPrice()));

        savedProduct.setReservedStock(0.0);
        return savedProduct;
    }

    @Transactional
    public Product updateProduct(Long id, ProductRequest request) {
        validatePrices(request);
        GstCalculator.rate(request.getGstRate());
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + id));

        // Check for purchase price change
        if (product.getPurchasePrice().compareTo(request.getPurchasePrice()) != 0) {
            product.setPurchasePrice(request.getPurchasePrice());
            priceHistoryRepository.save(new PriceHistory(product, PriceType.PURCHASE, request.getPurchasePrice()));
        }

        // Check for selling price change
        if (product.getSellingPrice().compareTo(request.getSellingPrice()) != 0) {
            product.setSellingPrice(request.getSellingPrice());
            priceHistoryRepository.save(new PriceHistory(product, PriceType.SELLING, request.getSellingPrice()));
        }

        product.setProductName(request.getProductName());
        product.setCategory(request.getCategory());
        product.setBrand(request.getBrand());
        product.setUnit(request.getUnit());
        // Stock quantity is decoupled from updateProduct to guarantee audited stock movements
        product.setMinimumStock(request.getMinimumStock());
        product.setGstRate(GstCalculator.rate(request.getGstRate()));
        product.setHsnCode(request.getHsnCode());

        Product saved = productRepository.save(product);
        saved.setReservedStock(productRepository.reservedQuantity(id));
        return saved;
    }

    public List<Product> listProducts(String search, String category) {
        if (search != null && !search.trim().isEmpty()) {
            return withAvailability(productRepository.findByProductNameContainingIgnoreCase(search));
        }
        if (category != null && !category.trim().isEmpty()) {
            return withAvailability(productRepository.findByCategoryIgnoreCase(category));
        }
        return withAvailability(productRepository.findAll());
    }

    private List<Product> withAvailability(List<Product> products) {
        if (products.isEmpty()) return products;
        java.util.Map<Long, Double> reserved = new java.util.HashMap<>();
        for (Object[] row : productRepository.reservedQuantities(products.stream().map(Product::getId).toList()))
            reserved.put(((Number) row[0]).longValue(), ((Number) row[1]).doubleValue());
        for (Product product : products) product.setReservedStock(reserved.getOrDefault(product.getId(), 0.0));
        return products;
    }

    public Product getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + id));
        product.setReservedStock(productRepository.reservedQuantity(id));
        return product;
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
