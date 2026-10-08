package com.riceerp.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riceerp.backend.dto.ReconciliationItemDetail;
import com.riceerp.backend.entity.*;
import com.riceerp.backend.enums.InvoiceStatus;
import com.riceerp.backend.enums.ReconciliationStatus;
import com.riceerp.backend.exception.BusinessRuleException;
import com.riceerp.backend.exception.NotFoundException;
import com.riceerp.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class ReconciliationService {
    private final ReconciliationResultRepository reconciliationResultRepository;
    private final PurchaseRepository purchaseRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final SupplierInvoiceService invoiceService;
    private final GoodsReceiptService goodsReceiptService;
    private final ObjectMapper objectMapper;
    private final ProcurementLock procurementLock;

    public ReconciliationService(ReconciliationResultRepository results, PurchaseRepository purchases,
            PurchaseItemRepository items, SupplierInvoiceService invoices, GoodsReceiptService receipts,
            ObjectMapper mapper, ProcurementLock procurementLock) {
        this.reconciliationResultRepository = results; this.purchaseRepository = purchases;
        this.purchaseItemRepository = items; this.invoiceService = invoices;
        this.goodsReceiptService = receipts; this.objectMapper = mapper; this.procurementLock = procurementLock;
    }

    private static BigDecimal value(double n) {
        if (!Double.isFinite(n) || n < 0) throw new BusinessRuleException("Reconciliation requires finite, non-negative quantities and prices.");
        return BigDecimal.valueOf(n);
    }
    private static BigDecimal value(BigDecimal n) {
        if (n == null || n.signum() < 0) throw new BusinessRuleException("Reconciliation requires non-negative amounts.");
        return n;
    }
    private static BigDecimal rounded(BigDecimal n) { return n.setScale(2, RoundingMode.HALF_UP); }
    private static BigDecimal storedAmount(BigDecimal amount) {
        BigDecimal rounded = rounded(value(amount));
        if (rounded.compareTo(new BigDecimal("999999999999999.9999")) > 0)
            throw new BusinessRuleException("Reconciliation amount exceeds the supported range.");
        return rounded;
    }
    private static class Totals {
        BigDecimal quantity = BigDecimal.ZERO, amount = BigDecimal.ZERO;
        String name;
        Map<BigDecimal, BigDecimal> byPrice = new TreeMap<>();
        void add(Product product, double qty, double price) {
            add(product, qty, value(price));
        }
        void add(Product product, double qty, BigDecimal price) {
            if (price == null || price.signum() < 0)
                throw new BusinessRuleException("Reconciliation requires non-negative prices.");
            BigDecimal q = value(qty), p = price;
            if (q.signum() <= 0) throw new BusinessRuleException("Invoice and purchase quantities must be positive.");
            name = product.getProductName(); quantity = quantity.add(q); amount = amount.add(q.multiply(p));
            byPrice.merge(p, q, BigDecimal::add);
        }
        BigDecimal average() { return quantity.signum() == 0 ? BigDecimal.ZERO : amount.divide(quantity, 6, RoundingMode.HALF_UP); }
    }
    private Map<Long, Totals> invoiceTotals(Long id) {
        Map<Long, Totals> totals = new LinkedHashMap<>();
        for (SupplierInvoiceItem item : invoiceService.getInvoiceItems(id)) {
            if (item.getProduct() == null) throw new BusinessRuleException("Invoice product is missing.");
            Totals group = totals.computeIfAbsent(item.getProduct().getId(), k -> new Totals());
            BigDecimal previousAmount = group.amount;
            group.add(item.getProduct(), item.getQuantity(), item.getUnitPrice());
            // Invoice headers sum lines at the database's four-decimal precision.
            BigDecimal lineAmount = value(item.getQuantity()).multiply(value(item.getUnitPrice()))
                    .setScale(4, RoundingMode.HALF_UP);
            group.amount = previousAmount.add(lineAmount);
        }
        if (totals.isEmpty()) throw new BusinessRuleException("Invoice must contain at least one item.");
        return totals;
    }
    private BigDecimal amount(Map<Long, Totals> groups) {
        return groups.values().stream().map(t -> t.amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    private void sameSupplier(Purchase purchase, SupplierInvoice invoice) {
        if (invoice.getSupplier() == null || purchase.getSupplier() == null ||
                !Objects.equals(invoice.getSupplier().getId(), purchase.getSupplier().getId()))
            throw new BusinessRuleException("Invoice supplier does not match the purchase supplier.");
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReconciliationResult reconcile(Long purchaseId, Long invoiceId) {
        Long orgId = procurementLock.acquire();
        Purchase purchase = purchaseRepository.findByIdAndOrganizationId(purchaseId, orgId)
                .orElseThrow(() -> new NotFoundException("Purchase not found"));
        SupplierInvoice invoice = invoiceService.getInvoiceById(invoiceId);
        sameSupplier(purchase, invoice);
        if (invoice.getPurchase() != null && !Objects.equals(invoice.getPurchase().getId(), purchaseId))
            throw new BusinessRuleException("Invoice is already linked to another purchase.");

        Map<Long, Totals> ordered = new LinkedHashMap<>();
        for (PurchaseItem item : purchaseItemRepository.findByPurchaseId(purchaseId))
            ordered.computeIfAbsent(item.getProduct().getId(), k -> new Totals()).add(item.getProduct(), item.getQuantity(), item.getPrice());
        if (ordered.isEmpty()) throw new BusinessRuleException("Purchase has no items to reconcile.");
        Map<Long, Totals> current = invoiceTotals(invoiceId), previous = new LinkedHashMap<>();
        boolean headerMatches = rounded(amount(current)).compareTo(rounded(value(invoice.getTotalAmount()))) == 0;
        boolean allMatch = headerMatches;
        // Every linked invoice consumes capacity, including PAID/MISMATCHED ones.
        // Reconciliation results are audit snapshots, never additional consumption.
        for (SupplierInvoice other : invoiceService.getInvoicesForPurchase(purchaseId)) {
            if (Objects.equals(other.getId(), invoiceId)) continue;
            sameSupplier(purchase, other);
            Map<Long, Totals> groups = invoiceTotals(other.getId());
            if (rounded(amount(groups)).compareTo(rounded(value(other.getTotalAmount()))) != 0) allMatch = false;
            for (var entry : groups.entrySet()) {
                Totals from = entry.getValue(), to = previous.computeIfAbsent(entry.getKey(), k -> new Totals());
                to.name = from.name; to.quantity = to.quantity.add(from.quantity); to.amount = to.amount.add(from.amount);
                from.byPrice.forEach((price, qty) -> to.byPrice.merge(price, qty, BigDecimal::add));
            }
        }
        Map<Long, Double> received = goodsReceiptService.getReceivedQuantities(purchaseId);
        Set<Long> products = new LinkedHashSet<>(current.keySet()); products.addAll(previous.keySet());
        List<ReconciliationItemDetail> details = new ArrayList<>();
        BigDecimal matched = BigDecimal.ZERO;
        for (Long productId : products) {
            Totals po = ordered.getOrDefault(productId, new Totals());
            Totals now = current.getOrDefault(productId, new Totals());
            Totals before = previous.getOrDefault(productId, new Totals());
            BigDecimal receipts = value(received.getOrDefault(productId, 0.0));
            BigDecimal cumulative = now.quantity.add(before.quantity);
            boolean qtyMatch = ordered.containsKey(productId) && cumulative.compareTo(po.quantity) <= 0 && cumulative.compareTo(receipts) <= 0;
            Map<BigDecimal, BigDecimal> billedByPrice = new TreeMap<>(before.byPrice);
            now.byPrice.forEach((price, qty) -> billedByPrice.merge(price, qty, BigDecimal::add));
            boolean priceMatch = ordered.containsKey(productId) && billedByPrice.entrySet().stream()
                    .allMatch(entry -> entry.getValue().compareTo(po.byPrice.getOrDefault(entry.getKey(), BigDecimal.ZERO)) <= 0);
            if (!qtyMatch || !priceMatch) allMatch = false;
            // Only fully supported current product groups contribute matched value.
            if (qtyMatch && priceMatch) matched = matched.add(now.amount);
            ReconciliationItemDetail detail = new ReconciliationItemDetail();
            detail.setProductId(productId); detail.setProductName(now.name != null ? now.name : before.name);
            detail.setOrderedQty(po.quantity.doubleValue()); detail.setReceivedQty(receipts.doubleValue());
            detail.setBilledQty(now.quantity.doubleValue()); detail.setPreviouslyBilledQty(before.quantity.doubleValue());
            detail.setAvailableReceivedQty(receipts.subtract(before.quantity).max(BigDecimal.ZERO).doubleValue());
            detail.setOrderedPrice(po.average()); detail.setBilledPrice(now.average());
            detail.setOrderedAmount(rounded(po.amount)); detail.setBilledAmount(rounded(now.amount));
            detail.setQtyMatch(qtyMatch); detail.setPriceMatch(priceMatch); details.add(detail);
        }
        ReconciliationResult result = new ReconciliationResult();
        result.setPurchase(purchase); result.setInvoice(invoice);
        result.setStatus(allMatch ? ReconciliationStatus.MATCHED : ReconciliationStatus.MISMATCHED);
        result.setAmountMatched(headerMatches ? storedAmount(matched) : BigDecimal.ZERO);
        result.setAmountOnPurchase(storedAmount(amount(ordered)));
        result.setAmountOnInvoice(storedAmount(value(invoice.getTotalAmount())));
        result.setReconciledAt(LocalDateTime.now());
        try { result.setDetails(objectMapper.writeValueAsString(details)); }
        catch (JsonProcessingException ex) { throw new IllegalStateException("Could not save reconciliation detail", ex); }
        invoice.setPurchase(purchase);
        if (invoice.getStatus() != InvoiceStatus.PAID)
            invoice.setStatus(allMatch ? InvoiceStatus.MATCHED : InvoiceStatus.MISMATCHED);
        invoiceService.save(invoice);
        return reconciliationResultRepository.save(result);
    }

    public ReconciliationResult getById(Long id) {
        return reconciliationResultRepository.findById(id).orElseThrow(() -> new NotFoundException("Reconciliation not found with id: " + id));
    }
    public ReconciliationResult getForPurchase(Long purchaseId) {
        return reconciliationResultRepository.findByPurchaseIdOrderByReconciledAtDesc(purchaseId).stream()
                .findFirst().orElseThrow(() -> new NotFoundException("No reconciliation found for purchase: " + purchaseId));
    }
}
