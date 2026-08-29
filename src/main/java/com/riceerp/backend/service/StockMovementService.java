package com.riceerp.backend.service;

import com.riceerp.backend.entity.Product;
import com.riceerp.backend.entity.StockMovement;
import com.riceerp.backend.enums.MovementType;
import com.riceerp.backend.repository.StockMovementRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StockMovementService {

    private final StockMovementRepository stockMovementRepository;

    public StockMovementService(StockMovementRepository stockMovementRepository) {
        this.stockMovementRepository = stockMovementRepository;
    }

    public void record(Product product, MovementType movementType, double quantity, Long referenceId) {
        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setMovementType(movementType);
        movement.setQuantity(quantity);
        movement.setReferenceId(referenceId);
        movement.setCreatedAt(LocalDateTime.now());
        stockMovementRepository.save(movement);
    }

    public List<StockMovement> listMovements(Long productId, LocalDateTime start, LocalDateTime end) {
        if (productId != null && start != null && end != null) {
            return stockMovementRepository.findByProductIdAndCreatedAtBetween(productId, start, end);
        }
        if (productId != null) {
            return stockMovementRepository.findByProductId(productId);
        }
        if (start != null && end != null) {
            return stockMovementRepository.findByCreatedAtBetween(start, end);
        }
        return stockMovementRepository.findAll();
    }
}