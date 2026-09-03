package com.riceerp.backend.service;

import com.riceerp.backend.dto.PurchaseItemRequest;
import com.riceerp.backend.dto.PurchaseRequest;
import com.riceerp.backend.dto.PurchaseReturnRequest;
import com.riceerp.backend.entity.*;
import com.riceerp.backend.enums.MovementType;
import com.riceerp.backend.enums.PurchaseStatus;
import com.riceerp.backend.exception.BusinessRuleException;
import com.riceerp.backend.exception.NotFoundException;
import com.riceerp.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class PurchaseService {

    private static final Map<PurchaseStatus, Set<PurchaseStatus>> TRANSITIONS = new EnumMap<>(PurchaseStatus.class);

    static {
        TRANSITIONS.put(PurchaseStatus.DRAFT, EnumSet.of(PurchaseStatus.PENDING_APPROVAL, PurchaseStatus.CANCELLED));
        TRANSITIONS.put(PurchaseStatus.PENDING_APPROVAL, EnumSet.of(PurchaseStatus.APPROVED, PurchaseStatus.CANCELLED));
        TRANSITIONS.put(PurchaseStatus.APPROVED, EnumSet.of(PurchaseStatus.ORDERED, PurchaseStatus.CANCELLED));
        TRANSITIONS.put(PurchaseStatus.ORDERED, EnumSet.of(PurchaseStatus.PARTIALLY_RECEIVED, PurchaseStatus.RECEIVED, PurchaseStatus.CANCELLED));
        TRANSITIONS.put(PurchaseStatus.PARTIALLY_RECEIVED, EnumSet.of(PurchaseStatus.RECEIVED, PurchaseStatus.CANCELLED));
        TRANSITIONS.put(PurchaseStatus.RECEIVED, EnumSet.of(PurchaseStatus.COMPLETED, PurchaseStatus.CANCELLED));
        TRANSITIONS.put(PurchaseStatus.COMPLETED, EnumSet.noneOf(PurchaseStatus.class));
        TRANSITIONS.put(PurchaseStatus.CANCELLED, EnumSet.noneOf(PurchaseStatus.class));
    }

    private final PurchaseRepository purchaseRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final PurchaseReturnRepository purchaseReturnRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final StockMovementService stockMovementService;

    public PurchaseService(
            PurchaseRepository purchaseRepository,
            PurchaseItemRepository purchaseItemRepository,
            PurchaseReturnRepository purchaseReturnRepository,
            SupplierRepository supplierRepository,
            ProductRepository productRepository,
            StockMovementService stockMovementService) {
        this.purchaseRepository = purchaseRepository;
        this.purchaseItemRepository = purchaseItemRepository;
        this.purchaseReturnRepository = purchaseReturnRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.stockMovementService = stockMovementService;
    }

    public static boolean canTransition(PurchaseStatus from, PurchaseStatus to) {
        return TRANSITIONS.getOrDefault(from, Collections.emptySet()).contains(to);
    }

    private void assertTransition(Purchase purchase, PurchaseStatus target) {
        if (purchase.getStatus() == target) {
            throw new BusinessRuleException("Purchase is already in status: " + target);
        }
        if (!canTransition(purchase.getStatus(), target)) {
            throw new BusinessRuleException("Invalid status transition from " + purchase.getStatus()
                    + " to " + target);
        }
    }

    @Transactional
    public Purchase createPurchase(PurchaseRequest request) {
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new NotFoundException("Supplier not found with id: " + request.getSupplierId()));

        Purchase purchase = new Purchase();
        purchase.setSupplier(supplier);
        purchase.setInvoiceNumber(request.getInvoiceNumber());
        purchase.setStatus(request.getStatus() != null ? request.getStatus() : PurchaseStatus.DRAFT);
        purchase.setPurchaseDate(LocalDateTime.now());

        // Save initial empty total to generate ID
        purchase.setTotalAmount(0.0);
        Purchase savedPurchase = purchaseRepository.save(purchase);

        double totalAmount = 0.0;
        List<PurchaseItem> items = new ArrayList<>();

        for (PurchaseItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new NotFoundException("Product not found with id: " + itemReq.getProductId()));

            PurchaseItem item = new PurchaseItem();
            item.setPurchase(savedPurchase);
            item.setProduct(product);
            item.setQuantity(itemReq.getQuantity());
            item.setPrice(itemReq.getPrice());
            purchaseItemRepository.save(item);

            totalAmount += (itemReq.getQuantity() * itemReq.getPrice());
        }

        savedPurchase.setTotalAmount(totalAmount);
        return purchaseRepository.save(savedPurchase);
    }

    @Transactional
    public Purchase updateStatus(Long id, PurchaseStatus target) {
        Purchase purchase = getPurchaseById(id);
        assertTransition(purchase, target);
        purchase.setStatus(target);
        return purchaseRepository.save(purchase);
    }

    @Transactional
    public Purchase submit(Long id) {
        return updateStatus(id, PurchaseStatus.PENDING_APPROVAL);
    }

    @Transactional
    public Purchase approve(Long id) {
        return updateStatus(id, PurchaseStatus.APPROVED);
    }

    @Transactional
    public Purchase order(Long id) {
        return updateStatus(id, PurchaseStatus.ORDERED);
    }

    @Transactional
    public Purchase cancel(Long id) {
        return updateStatus(id, PurchaseStatus.CANCELLED);
    }

    @Transactional
    public PurchaseReturn createPurchaseReturn(Long purchaseId, PurchaseReturnRequest request) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new NotFoundException("Purchase not found with id: " + purchaseId));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + request.getProductId()));

        // Never drive stock negative via a purchase return.
        if (product.getStock() < request.getQuantityReturned()) {
            throw new BusinessRuleException("Cannot return more than available stock for product: "
                    + product.getProductName() + " (Available: " + product.getStock()
                    + ", Requested: " + request.getQuantityReturned() + ")");
        }

        // Subtract Stock
        product.setStock(product.getStock() - request.getQuantityReturned());
        productRepository.save(product);

        // Stock movement ledger
        stockMovementService.record(product, MovementType.RETURN, -request.getQuantityReturned(), purchaseId);

        PurchaseReturn pReturn = new PurchaseReturn();
        pReturn.setPurchase(purchase);
        pReturn.setProduct(product);
        pReturn.setQuantityReturned(request.getQuantityReturned());
        pReturn.setReason(request.getReason());
        pReturn.setReturnDate(LocalDateTime.now());

        return purchaseReturnRepository.save(pReturn);
    }

    public Purchase getPurchaseById(Long id) {
        return purchaseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Purchase not found with id: " + id));
    }

    public List<Purchase> listPurchases(Long supplierId, String invoice) {
        if (supplierId != null) {
            return purchaseRepository.findBySupplierId(supplierId);
        }
        if (invoice != null && !invoice.trim().isEmpty()) {
            return purchaseRepository.findByInvoiceNumberContainingIgnoreCase(invoice);
        }
        return purchaseRepository.findAll();
    }

    public List<PurchaseItem> getPurchaseItems(Long purchaseId) {
        return purchaseItemRepository.findByPurchaseId(purchaseId);
    }

    public List<PurchaseReturn> getPurchaseReturns(Long purchaseId) {
        return purchaseReturnRepository.findByPurchaseId(purchaseId);
    }
}