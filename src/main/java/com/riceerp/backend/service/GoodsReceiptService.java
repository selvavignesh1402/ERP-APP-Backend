package com.riceerp.backend.service;

import com.riceerp.backend.dto.GoodsReceiptItemRequest;
import com.riceerp.backend.dto.GoodsReceiptRequest;
import com.riceerp.backend.entity.*;
import com.riceerp.backend.enums.MovementType;
import com.riceerp.backend.enums.PurchaseStatus;
import com.riceerp.backend.repository.*;
import com.riceerp.backend.exception.BusinessRuleException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GoodsReceiptService {

    private final GoodsReceiptRepository goodsReceiptRepository;
    private final GoodsReceiptItemRepository goodsReceiptItemRepository;
    private final PurchaseRepository purchaseRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final ProductRepository productRepository;
    private final StockMovementService stockMovementService;
    private final ProcurementLock procurementLock;

    public GoodsReceiptService(
            GoodsReceiptRepository goodsReceiptRepository,
            GoodsReceiptItemRepository goodsReceiptItemRepository,
            PurchaseRepository purchaseRepository,
            PurchaseItemRepository purchaseItemRepository,
            ProductRepository productRepository,
            StockMovementService stockMovementService, ProcurementLock procurementLock) {
        this.goodsReceiptRepository = goodsReceiptRepository;
        this.goodsReceiptItemRepository = goodsReceiptItemRepository;
        this.purchaseRepository = purchaseRepository;
        this.purchaseItemRepository = purchaseItemRepository;
        this.productRepository = productRepository;
        this.stockMovementService = stockMovementService;
        this.procurementLock = procurementLock;
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public GoodsReceipt createReceipt(Long purchaseId, GoodsReceiptRequest request) {
        Long orgId = procurementLock.acquire();
        Purchase purchase = purchaseRepository.findByIdAndOrganizationId(purchaseId, orgId)
                .orElseThrow(() -> new com.riceerp.backend.exception.NotFoundException("Purchase not found with id: " + purchaseId));

        if (purchase.getStatus() != PurchaseStatus.ORDERED
                && purchase.getStatus() != PurchaseStatus.PARTIALLY_RECEIVED) {
            throw new BusinessRuleException("Goods can only be received when the purchase is ORDERED or PARTIALLY_RECEIVED. "
                    + "Current status: " + purchase.getStatus());
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessRuleException("Receipt must contain at least one item.");
        }

        List<PurchaseItem> purchaseItems = purchaseItemRepository.findByPurchaseId(purchaseId);
        Map<Long, BigDecimal> orderedByProduct = PurchaseQuantities.ordered(purchaseItems);
        Map<Long, PurchaseItem> productLines = new HashMap<>();
        for (PurchaseItem poItem : purchaseItems) {
            productLines.putIfAbsent(poItem.getProduct().getId(), poItem);
        }

        Map<Long, BigDecimal> receivedSoFar = receivedQuantities(purchaseId);
        Map<Long, BigDecimal> receivedNow = new HashMap<>();
        // Validate the complete receipt before any writes, including repeated products.
        for (GoodsReceiptItemRequest item : request.getItems()) {
            if (item == null || !orderedByProduct.containsKey(item.getProductId()))
                throw new BusinessRuleException("Each receipt item must specify a product from this purchase.");
            receivedNow.merge(item.getProductId(), PurchaseQuantities.positive(item.getReceivedQty()), BigDecimal::add);
            BigDecimal price = item.getUnitPrice();
            if (price == null || price.signum() < 0 || price.compareTo(new BigDecimal("999999999999999.9999")) > 0
                    || price.stripTrailingZeros().scale() > 4)
                throw new BusinessRuleException("Receipt price must be non-negative, within the supported range, and have at most four decimal places.");
            if (price.signum() == 0 && productLines.get(item.getProductId()).getPrice() == null)
                throw new BusinessRuleException("Purchase price is missing. Specify a receipt price.");
        }
        for (var entry : receivedNow.entrySet()) {
            Long productId = entry.getKey();
            BigDecimal cumulative = receivedSoFar.getOrDefault(productId, BigDecimal.ZERO).add(entry.getValue());
            if (cumulative.compareTo(orderedByProduct.get(productId)) > 0)
                throw new BusinessRuleException("Over-receiving not allowed for product "
                        + productLines.get(productId).getProduct().getProductName() + ". Ordered: "
                        + orderedByProduct.get(productId) + ", Total received including this receipt: " + cumulative);
            PurchaseQuantities.stored(PurchaseQuantities.nonNegative(productLines.get(productId).getProduct().getStock()).add(entry.getValue()));
            receivedSoFar.put(productId, cumulative);
        }

        GoodsReceipt receipt = new GoodsReceipt();
        receipt.setPurchase(purchase);
        receipt.setReceiptNumber(request.getReceiptNumber() != null && !request.getReceiptNumber().trim().isEmpty()
                ? request.getReceiptNumber()
                : "GRN-" + System.currentTimeMillis());
        receipt.setReceivedDate(LocalDateTime.now());
        GoodsReceipt savedReceipt = goodsReceiptRepository.save(receipt);

        for (GoodsReceiptItemRequest itemReq : request.getItems()) {
            PurchaseItem poItem = productLines.get(itemReq.getProductId());
            GoodsReceiptItem item = new GoodsReceiptItem();
            item.setReceipt(savedReceipt);
            item.setProduct(poItem.getProduct());
            item.setOrderedQty(PurchaseQuantities.stored(orderedByProduct.get(itemReq.getProductId())));
            item.setReceivedQty(itemReq.getReceivedQty());
            item.setUnitPrice(itemReq.getUnitPrice().signum() > 0 ? itemReq.getUnitPrice() : poItem.getPrice());
            goodsReceiptItemRepository.save(item);

        }

        // One inventory update and movement for the total received for each product.
        for (var entry : receivedNow.entrySet()) {
            Product product = productLines.get(entry.getKey()).getProduct();
            product.setStock(PurchaseQuantities.stored(PurchaseQuantities.nonNegative(product.getStock()).add(entry.getValue())));
            productRepository.save(product);
            stockMovementService.record(product, MovementType.PURCHASE_RECEIPT, PurchaseQuantities.stored(entry.getValue()), savedReceipt.getId());
        }

        // Recompute purchase status based on aggregate receiving
        boolean allReceived = orderedByProduct.entrySet().stream().allMatch(entry ->
                receivedSoFar.getOrDefault(entry.getKey(), BigDecimal.ZERO).compareTo(entry.getValue()) == 0);
        purchase.setStatus(allReceived ? PurchaseStatus.RECEIVED : PurchaseStatus.PARTIALLY_RECEIVED);
        purchaseRepository.save(purchase);

        return savedReceipt;
    }

    public List<GoodsReceipt> listReceiptsForPurchase(Long purchaseId) {
        return goodsReceiptRepository.findByPurchaseId(purchaseId);
    }

    public GoodsReceipt getReceiptById(Long receiptId) {
        return goodsReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new com.riceerp.backend.exception.NotFoundException("Goods receipt not found with id: " + receiptId));
    }

    public List<GoodsReceiptItem> getReceiptItems(Long receiptId) {
        return goodsReceiptItemRepository.findByReceiptId(receiptId);
    }

    public Map<Long, Double> getReceivedQuantities(Long purchaseId) {
        Map<Long, Double> quantities = new HashMap<>();
        receivedQuantities(purchaseId).forEach((id, quantity) -> quantities.put(id, PurchaseQuantities.stored(quantity)));
        return quantities;
    }

    private Map<Long, BigDecimal> receivedQuantities(Long purchaseId) {
        Map<Long, BigDecimal> receivedSoFar = new HashMap<>();
        for (GoodsReceipt existing : goodsReceiptRepository.findByPurchaseId(purchaseId)) {
            for (GoodsReceiptItem existingItem : goodsReceiptItemRepository.findByReceiptId(existing.getId())) {
                receivedSoFar.merge(existingItem.getProduct().getId(), PurchaseQuantities.positive(existingItem.getReceivedQty()), BigDecimal::add);
            }
        }
        return receivedSoFar;
    }
}
