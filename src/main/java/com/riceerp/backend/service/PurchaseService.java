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
import org.springframework.transaction.annotation.Isolation;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.*;

@Service
public class PurchaseService {

    private static final Map<PurchaseStatus, Set<PurchaseStatus>> TRANSITIONS = new EnumMap<>(PurchaseStatus.class);

    static {
        TRANSITIONS.put(PurchaseStatus.DRAFT, EnumSet.of(PurchaseStatus.PENDING_APPROVAL, PurchaseStatus.CANCELLED));
        TRANSITIONS.put(PurchaseStatus.PENDING_APPROVAL, EnumSet.of(PurchaseStatus.APPROVED, PurchaseStatus.CANCELLED));
        TRANSITIONS.put(PurchaseStatus.APPROVED, EnumSet.of(PurchaseStatus.ORDERED, PurchaseStatus.CANCELLED));
        TRANSITIONS.put(PurchaseStatus.ORDERED, EnumSet.of(PurchaseStatus.CANCELLED));
        TRANSITIONS.put(PurchaseStatus.PARTIALLY_RECEIVED, EnumSet.noneOf(PurchaseStatus.class));
        TRANSITIONS.put(PurchaseStatus.RECEIVED, EnumSet.of(PurchaseStatus.COMPLETED));
        TRANSITIONS.put(PurchaseStatus.COMPLETED, EnumSet.noneOf(PurchaseStatus.class));
        TRANSITIONS.put(PurchaseStatus.CANCELLED, EnumSet.noneOf(PurchaseStatus.class));
    }

    private final PurchaseRepository purchaseRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final PurchaseReturnRepository purchaseReturnRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final StockMovementService stockMovementService;
    private final GoodsReceiptService goodsReceiptService;
    private final ProcurementLock procurementLock;

    public PurchaseService(
            PurchaseRepository purchaseRepository,
            PurchaseItemRepository purchaseItemRepository,
            PurchaseReturnRepository purchaseReturnRepository,
            SupplierRepository supplierRepository,
            ProductRepository productRepository,
            StockMovementService stockMovementService, GoodsReceiptService goodsReceiptService,
            ProcurementLock procurementLock) {
        this.purchaseRepository = purchaseRepository;
        this.purchaseItemRepository = purchaseItemRepository;
        this.purchaseReturnRepository = purchaseReturnRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.stockMovementService = stockMovementService;
        this.goodsReceiptService = goodsReceiptService;
        this.procurementLock = procurementLock;
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
        if (request.getItems() == null || request.getItems().isEmpty())
            throw new BusinessRuleException("Purchase must contain at least one item.");
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new NotFoundException("Supplier not found with id: " + request.getSupplierId()));

        Purchase purchase = new Purchase();
        purchase.setSupplier(supplier);
        purchase.setInvoiceNumber(request.getInvoiceNumber());
        // Creation permission never grants approval or receiving permission.
        purchase.setStatus(PurchaseStatus.DRAFT);
        purchase.setPurchaseDate(LocalDateTime.now());

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal maximum = new BigDecimal("999999999999999.9999");
        List<PurchaseItem> items = new ArrayList<>();

        for (PurchaseItemRequest itemReq : request.getItems()) {
            if (itemReq == null || itemReq.getProductId() == null)
                throw new BusinessRuleException("Each purchase item must specify a product.");
            BigDecimal quantity = PurchaseQuantities.positive(itemReq.getQuantity());
            BigDecimal price = itemReq.getPrice();
            if (quantity.compareTo(new BigDecimal("9999999999999.999999")) > 0 || quantity.stripTrailingZeros().scale() > 6)
                throw new BusinessRuleException("Purchase quantity is too large or has more than six decimal places.");
            if (price == null || price.signum() <= 0 || price.compareTo(maximum) > 0 || price.stripTrailingZeros().scale() > 4)
                throw new BusinessRuleException("Purchase price must be positive, within the supported range, and have at most four decimal places.");
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new NotFoundException("Product not found with id: " + itemReq.getProductId()));

            PurchaseItem item = new PurchaseItem();
            item.setProduct(product);
            item.setQuantity(itemReq.getQuantity());
            item.setPrice(itemReq.getPrice());
            items.add(item);
            totalAmount = totalAmount.add(quantity.multiply(price));
        }

        // Match the existing four-decimal storage precision explicitly before persisting.
        totalAmount = totalAmount.setScale(4, java.math.RoundingMode.HALF_UP);
        if (totalAmount.compareTo(maximum) > 0)
            throw new BusinessRuleException("Purchase total exceeds the supported amount.");
        purchase.setTotalAmount(totalAmount);
        Purchase savedPurchase = purchaseRepository.save(purchase);
        for (PurchaseItem item : items) {
            item.setPurchase(savedPurchase);
            purchaseItemRepository.save(item);
        }
        return savedPurchase;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Purchase updateStatus(Long id, PurchaseStatus target) {
        Long orgId = procurementLock.acquire();
        if (target == null) {
            throw new BusinessRuleException("Purchase status is required.");
        }
        if (target == PurchaseStatus.PARTIALLY_RECEIVED || target == PurchaseStatus.RECEIVED) {
            throw new BusinessRuleException("Record a goods receipt to update the purchase receiving status.");
        }
        Purchase purchase = purchaseRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new NotFoundException("Purchase not found with id: " + id));
        if (target == PurchaseStatus.CANCELLED && !goodsReceiptService.listReceiptsForPurchase(id).isEmpty())
            throw new BusinessRuleException("A purchase with goods receipts cannot be cancelled. Record a purchase return for goods sent back.");
        assertTransition(purchase, target);
        if (target == PurchaseStatus.COMPLETED) {
            Map<Long, BigDecimal> ordered = PurchaseQuantities.ordered(purchaseItemRepository.findByPurchaseId(id));
            Map<Long, Double> received = goodsReceiptService.getReceivedQuantities(id);
            if (!ordered.keySet().equals(received.keySet()) || ordered.entrySet().stream().anyMatch(entry ->
                    entry.getValue().compareTo(PurchaseQuantities.nonNegative(received.getOrDefault(entry.getKey(), 0.0))) != 0))
                throw new BusinessRuleException("Complete receiving all ordered goods before completing the purchase.");
        }
        purchase.setStatus(target);
        return purchaseRepository.save(purchase);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Purchase submit(Long id) {
        return updateStatus(id, PurchaseStatus.PENDING_APPROVAL);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Purchase approve(Long id) {
        return updateStatus(id, PurchaseStatus.APPROVED);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Purchase order(Long id) {
        return updateStatus(id, PurchaseStatus.ORDERED);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Purchase cancel(Long id) {
        return updateStatus(id, PurchaseStatus.CANCELLED);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PurchaseReturn createPurchaseReturn(Long purchaseId, PurchaseReturnRequest request) {
        Long orgId = procurementLock.acquire();
        BigDecimal returning = PurchaseQuantities.positive(request.getQuantityReturned());
        Purchase purchase = purchaseRepository.findByIdAndOrganizationId(purchaseId, orgId)
                .orElseThrow(() -> new NotFoundException("Purchase not found with id: " + purchaseId));
        if (purchase.getStatus() != PurchaseStatus.PARTIALLY_RECEIVED && purchase.getStatus() != PurchaseStatus.RECEIVED
                && purchase.getStatus() != PurchaseStatus.COMPLETED)
            throw new BusinessRuleException("Returns require a received or partially received purchase.");
        Map<Long, BigDecimal> ordered = PurchaseQuantities.ordered(purchaseItemRepository.findByPurchaseId(purchaseId));
        if (!ordered.containsKey(request.getProductId()))
            throw new BusinessRuleException("This product is not part of the purchase.");
        BigDecimal received = PurchaseQuantities.nonNegative(goodsReceiptService.getReceivedQuantities(purchaseId)
                .getOrDefault(request.getProductId(), 0.0));
        BigDecimal returned = BigDecimal.ZERO;
        for (PurchaseReturn previous : purchaseReturnRepository.findByPurchaseIdAndOrganizationId(purchaseId, orgId)) {
            if (Objects.equals(previous.getProduct().getId(), request.getProductId()))
                returned = returned.add(PurchaseQuantities.positive(previous.getQuantityReturned()));
        }
        BigDecimal remaining = received.subtract(returned);
        if (returning.compareTo(remaining) > 0)
            throw new BusinessRuleException("Cannot return more than received on this purchase minus earlier returns. Remaining: "
                    + remaining.max(BigDecimal.ZERO).toPlainString());
        Product product = productRepository.findForStockUpdate(request.getProductId())
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + request.getProductId()));

        // Never drive stock negative via a purchase return.
        BigDecimal stock = PurchaseQuantities.nonNegative(product.getStock());
        BigDecimal available = stock.subtract(BigDecimal.valueOf(productRepository.reservedQuantity(product.getId())));
        if (available.compareTo(returning) < 0) {
            throw new BusinessRuleException("Cannot return more than available stock for product: "
                    + product.getProductName() + " (Available: " + product.getStock()
                    + ", Requested: " + request.getQuantityReturned() + ")");
        }

        // Subtract Stock
        product.setStock(PurchaseQuantities.stored(stock.subtract(returning)));
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
