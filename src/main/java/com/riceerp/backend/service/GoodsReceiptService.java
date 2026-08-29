package com.riceerp.backend.service;

import com.riceerp.backend.dto.GoodsReceiptItemRequest;
import com.riceerp.backend.dto.GoodsReceiptRequest;
import com.riceerp.backend.entity.*;
import com.riceerp.backend.enums.MovementType;
import com.riceerp.backend.enums.PurchaseStatus;
import com.riceerp.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    public GoodsReceiptService(
            GoodsReceiptRepository goodsReceiptRepository,
            GoodsReceiptItemRepository goodsReceiptItemRepository,
            PurchaseRepository purchaseRepository,
            PurchaseItemRepository purchaseItemRepository,
            ProductRepository productRepository,
            StockMovementService stockMovementService) {
        this.goodsReceiptRepository = goodsReceiptRepository;
        this.goodsReceiptItemRepository = goodsReceiptItemRepository;
        this.purchaseRepository = purchaseRepository;
        this.purchaseItemRepository = purchaseItemRepository;
        this.productRepository = productRepository;
        this.stockMovementService = stockMovementService;
    }

    @Transactional
    public GoodsReceipt createReceipt(Long purchaseId, GoodsReceiptRequest request) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new RuntimeException("Purchase not found with id: " + purchaseId));

        if (purchase.getStatus() != PurchaseStatus.ORDERED
                && purchase.getStatus() != PurchaseStatus.PARTIALLY_RECEIVED) {
            throw new RuntimeException("Goods can only be received when the purchase is ORDERED or PARTIALLY_RECEIVED. "
                    + "Current status: " + purchase.getStatus());
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("Receipt must contain at least one item.");
        }

        List<PurchaseItem> purchaseItems = purchaseItemRepository.findByPurchaseId(purchaseId);
        Map<Long, PurchaseItem> orderedByProduct = new HashMap<>();
        for (PurchaseItem poItem : purchaseItems) {
            orderedByProduct.put(poItem.getProduct().getId(), poItem);
        }

        // Cumulative received quantity per product across all prior receipts
        Map<Long, Double> receivedSoFar = new HashMap<>();
        for (GoodsReceipt existing : goodsReceiptRepository.findByPurchaseId(purchaseId)) {
            for (GoodsReceiptItem existingItem : goodsReceiptItemRepository.findByReceiptId(existing.getId())) {
                Long pid = existingItem.getProduct().getId();
                receivedSoFar.merge(pid, existingItem.getReceivedQty(), Double::sum);
            }
        }

        GoodsReceipt receipt = new GoodsReceipt();
        receipt.setPurchase(purchase);
        receipt.setReceiptNumber(request.getReceiptNumber() != null && !request.getReceiptNumber().trim().isEmpty()
                ? request.getReceiptNumber()
                : "GRN-" + System.currentTimeMillis());
        receipt.setReceivedDate(LocalDateTime.now());
        GoodsReceipt savedReceipt = goodsReceiptRepository.save(receipt);

        for (GoodsReceiptItemRequest itemReq : request.getItems()) {
            PurchaseItem poItem = orderedByProduct.get(itemReq.getProductId());
            if (poItem == null) {
                throw new RuntimeException("Product id " + itemReq.getProductId()
                        + " is not part of this purchase.");
            }

            double ordered = poItem.getQuantity();
            double receivedNow = itemReq.getReceivedQty();
            if (receivedNow <= 0) {
                throw new RuntimeException("Received quantity must be greater than zero for product "
                        + poItem.getProduct().getProductName());
            }

            double cumulative = receivedSoFar.getOrDefault(itemReq.getProductId(), 0.0);
            if (cumulative + receivedNow > ordered) {
                throw new RuntimeException("Over-receiving not allowed for product "
                        + poItem.getProduct().getProductName() + ". Ordered: " + ordered
                        + ", Already received: " + cumulative + ", Attempting: " + receivedNow);
            }

            GoodsReceiptItem item = new GoodsReceiptItem();
            item.setReceipt(savedReceipt);
            item.setProduct(poItem.getProduct());
            item.setOrderedQty(ordered);
            item.setReceivedQty(receivedNow);
            item.setUnitPrice(itemReq.getUnitPrice() > 0 ? itemReq.getUnitPrice() : poItem.getPrice());
            goodsReceiptItemRepository.save(item);

            // Inventory increases only on goods receipt
            Product product = poItem.getProduct();
            product.setStock(product.getStock() + receivedNow);
            productRepository.save(product);

            // Stock movement ledger
            stockMovementService.record(product, MovementType.PURCHASE_RECEIPT, receivedNow, savedReceipt.getId());

            receivedSoFar.merge(itemReq.getProductId(), receivedNow, Double::sum);
        }

        // Recompute purchase status based on aggregate receiving
        boolean allReceived = true;
        for (PurchaseItem poItem : purchaseItems) {
            double cumulative = receivedSoFar.getOrDefault(poItem.getProduct().getId(), 0.0);
            if (cumulative < poItem.getQuantity()) {
                allReceived = false;
                break;
            }
        }
        purchase.setStatus(allReceived ? PurchaseStatus.RECEIVED : PurchaseStatus.PARTIALLY_RECEIVED);
        purchaseRepository.save(purchase);

        return savedReceipt;
    }

    public List<GoodsReceipt> listReceiptsForPurchase(Long purchaseId) {
        return goodsReceiptRepository.findByPurchaseId(purchaseId);
    }

    public GoodsReceipt getReceiptById(Long receiptId) {
        return goodsReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new RuntimeException("Goods receipt not found with id: " + receiptId));
    }

    public List<GoodsReceiptItem> getReceiptItems(Long receiptId) {
        return goodsReceiptItemRepository.findByReceiptId(receiptId);
    }

    public Map<Long, Double> getReceivedQuantities(Long purchaseId) {
        Map<Long, Double> receivedSoFar = new HashMap<>();
        for (GoodsReceipt existing : goodsReceiptRepository.findByPurchaseId(purchaseId)) {
            for (GoodsReceiptItem existingItem : goodsReceiptItemRepository.findByReceiptId(existing.getId())) {
                receivedSoFar.merge(existingItem.getProduct().getId(), existingItem.getReceivedQty(), Double::sum);
            }
        }
        return receivedSoFar;
    }
}