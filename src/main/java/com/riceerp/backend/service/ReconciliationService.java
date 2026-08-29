package com.riceerp.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riceerp.backend.dto.ReconciliationItemDetail;
import com.riceerp.backend.entity.Purchase;
import com.riceerp.backend.entity.PurchaseItem;
import com.riceerp.backend.entity.ReconciliationResult;
import com.riceerp.backend.entity.SupplierInvoice;
import com.riceerp.backend.entity.SupplierInvoiceItem;
import com.riceerp.backend.enums.ReconciliationStatus;
import com.riceerp.backend.repository.PurchaseItemRepository;
import com.riceerp.backend.repository.PurchaseRepository;
import com.riceerp.backend.repository.ReconciliationResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReconciliationService {

    private final ReconciliationResultRepository reconciliationResultRepository;
    private final PurchaseRepository purchaseRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final SupplierInvoiceService invoiceService;
    private final GoodsReceiptService goodsReceiptService;
    private final ObjectMapper objectMapper;

    public ReconciliationService(ReconciliationResultRepository reconciliationResultRepository,
                                 PurchaseRepository purchaseRepository,
                                 PurchaseItemRepository purchaseItemRepository,
                                 SupplierInvoiceService invoiceService,
                                 GoodsReceiptService goodsReceiptService,
                                 ObjectMapper objectMapper) {
        this.reconciliationResultRepository = reconciliationResultRepository;
        this.purchaseRepository = purchaseRepository;
        this.purchaseItemRepository = purchaseItemRepository;
        this.invoiceService = invoiceService;
        this.goodsReceiptService = goodsReceiptService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ReconciliationResult reconcile(Long purchaseId, Long invoiceId) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new RuntimeException("Purchase not found with id: " + purchaseId));

        SupplierInvoice invoice = invoiceService.getInvoiceById(invoiceId);
        if (invoice.getPurchase() == null || !invoice.getPurchase().getId().equals(purchaseId)) {
            throw new RuntimeException("Invoice " + invoice.getInvoiceNumber() + " is not linked to purchase " + purchaseId + ".");
        }

        List<PurchaseItem> poItems = purchaseItemRepository.findByPurchaseId(purchaseId);
        Map<Long, SupplierInvoiceItem> invoiceByProduct = invoiceService.getInvoiceItems(invoiceId).stream()
                .collect(Collectors.toMap(i -> i.getProduct().getId(), i -> i));
        Map<Long, Double> receivedByProduct = goodsReceiptService.getReceivedQuantities(purchaseId);

        List<ReconciliationItemDetail> details = new ArrayList<>();
        boolean allMatch = true;
        double amountMatched = 0.0;

        for (PurchaseItem poItem : poItems) {
            Long productId = poItem.getProduct().getId();
            SupplierInvoiceItem invItem = invoiceByProduct.get(productId);
            double receivedQty = receivedByProduct.getOrDefault(productId, 0.0);
            double billedQty = invItem != null ? invItem.getQuantity() : 0.0;
            double billedPrice = invItem != null ? invItem.getUnitPrice() : 0.0;

            boolean qtyMatch = billedQty <= poItem.getQuantity() + 1e-9;
            boolean priceMatch = Math.abs(billedPrice - poItem.getPrice()) < 1e-9;
            if (invItem == null) {
                qtyMatch = false;
                priceMatch = false;
            }
            if (!qtyMatch || !priceMatch) {
                allMatch = false;
            }

            double matchedQty = Math.min(poItem.getQuantity(), Math.min(receivedQty, billedQty));
            amountMatched += matchedQty * poItem.getPrice();

            ReconciliationItemDetail detail = new ReconciliationItemDetail();
            detail.setProductId(productId);
            detail.setProductName(poItem.getProduct().getProductName());
            detail.setOrderedQty(poItem.getQuantity());
            detail.setReceivedQty(receivedQty);
            detail.setBilledQty(billedQty);
            detail.setOrderedPrice(poItem.getPrice());
            detail.setBilledPrice(billedPrice);
            detail.setQtyMatch(qtyMatch);
            detail.setPriceMatch(priceMatch);
            details.add(detail);
        }

        // Any invoice item not on the PO is a mismatch
        for (SupplierInvoiceItem invItem : invoiceService.getInvoiceItems(invoiceId)) {
            Long productId = invItem.getProduct().getId();
            boolean onPo = poItems.stream().anyMatch(pi -> pi.getProduct().getId().equals(productId));
            if (!onPo) {
                allMatch = false;
                ReconciliationItemDetail detail = new ReconciliationItemDetail();
                detail.setProductId(productId);
                detail.setProductName(invItem.getProduct().getProductName());
                detail.setOrderedQty(0);
                detail.setReceivedQty(receivedByProduct.getOrDefault(productId, 0.0));
                detail.setBilledQty(invItem.getQuantity());
                detail.setOrderedPrice(0);
                detail.setBilledPrice(invItem.getUnitPrice());
                detail.setQtyMatch(false);
                detail.setPriceMatch(false);
                details.add(detail);
            }
        }

        ReconciliationResult result = new ReconciliationResult();
        result.setPurchase(purchase);
        result.setInvoice(invoice);
        result.setStatus(allMatch ? ReconciliationStatus.MATCHED : ReconciliationStatus.MISMATCHED);
        result.setAmountMatched(round2(amountMatched));
        result.setAmountOnPurchase(round2(poItems.stream().mapToDouble(pi -> pi.getQuantity() * pi.getPrice()).sum()));
        result.setAmountOnInvoice(round2(invoice.getTotalAmount()));
        result.setReconciledAt(LocalDateTime.now());
        try {
            result.setDetails(objectMapper.writeValueAsString(details));
        } catch (JsonProcessingException e) {
            result.setDetails("[]");
        }
        return reconciliationResultRepository.save(result);
    }

    public ReconciliationResult getById(Long id) {
        return reconciliationResultRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reconciliation not found with id: " + id));
    }

    public ReconciliationResult getForPurchase(Long purchaseId) {
        return reconciliationResultRepository.findByPurchaseIdOrderByReconciledAtDesc(purchaseId).stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No reconciliation found for purchase: " + purchaseId));
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}