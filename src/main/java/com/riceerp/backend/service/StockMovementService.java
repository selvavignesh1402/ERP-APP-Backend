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

    public record MovementPage(List<StockMovement> items, Long nextBeforeId) {}

    public MovementPage movementPage(Long beforeId, int size) {
        Long orgId = com.riceerp.backend.security.TenantContext.getCurrentTenant();
        if (orgId == null || orgId <= 0) throw new org.springframework.security.access.AccessDeniedException("Select an organization first");
        if (size < 1 || size > 100 || (beforeId != null && beforeId <= 0))
            throw new com.riceerp.backend.exception.BusinessRuleException("Page size must be 1–100 and the movement cursor must be positive.");
        List<StockMovement> rows = stockMovementRepository.findByOrganizationIdAndIdLessThanOrderByIdDesc(
                orgId, beforeId == null ? Long.MAX_VALUE : beforeId, org.springframework.data.domain.PageRequest.of(0, size + 1));
        boolean more = rows.size() > size;
        List<StockMovement> items = List.copyOf(rows.subList(0, Math.min(size, rows.size())));
        return new MovementPage(items, more ? items.get(items.size() - 1).getId() : null);
    }
}
