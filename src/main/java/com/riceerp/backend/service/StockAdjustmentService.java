package com.riceerp.backend.service;

import com.riceerp.backend.dto.StockAdjustmentRequest;
import com.riceerp.backend.entity.Product;
import com.riceerp.backend.entity.StockAdjustment;
import com.riceerp.backend.enums.MovementType;
import com.riceerp.backend.exception.BusinessRuleException;
import com.riceerp.backend.exception.NotFoundException;
import com.riceerp.backend.repository.ProductRepository;
import com.riceerp.backend.repository.StockAdjustmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StockAdjustmentService {

    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final ProductRepository productRepository;
    private final StockMovementService stockMovementService;

    public StockAdjustmentService(StockAdjustmentRepository stockAdjustmentRepository,
            ProductRepository productRepository,
            StockMovementService stockMovementService) {
        this.stockAdjustmentRepository = stockAdjustmentRepository;
        this.productRepository = productRepository;
        this.stockMovementService = stockMovementService;
    }

    @Transactional
    public StockAdjustment createAdjustment(StockAdjustmentRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + request.getProductId()));

        double newStock = product.getStock() + request.getQuantityChange();
        if (newStock < 0) {
            throw new BusinessRuleException("Adjustment would cause negative stock (" + newStock + ") for product: "
                    + product.getProductName());
        }

        product.setStock(newStock);
        productRepository.save(product);

        // Stock movement ledger
        stockMovementService.record(product, MovementType.ADJUSTMENT, request.getQuantityChange(), null);

        StockAdjustment adjustment = new StockAdjustment();
        adjustment.setProduct(product);
        adjustment.setQuantityChange(request.getQuantityChange());
        adjustment.setReason(request.getReason());
        adjustment.setAdjustedAt(LocalDateTime.now());

        return stockAdjustmentRepository.save(adjustment);
    }

    public List<StockAdjustment> listAdjustmentsByProduct(Long productId) {
        if (productId != null) {
            return stockAdjustmentRepository.findByProductIdOrderByAdjustedAtDesc(productId);
        }
        return stockAdjustmentRepository.findAll();
    }
}