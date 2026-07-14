package com.riceerp.backend.service;

import com.riceerp.backend.dto.PurchaseItemRequest;
import com.riceerp.backend.dto.PurchaseRequest;
import com.riceerp.backend.dto.PurchaseReturnRequest;
import com.riceerp.backend.entity.*;
import com.riceerp.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final PurchaseReturnRepository purchaseReturnRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;

    public PurchaseService(
            PurchaseRepository purchaseRepository,
            PurchaseItemRepository purchaseItemRepository,
            PurchaseReturnRepository purchaseReturnRepository,
            SupplierRepository supplierRepository,
            ProductRepository productRepository) {
        this.purchaseRepository = purchaseRepository;
        this.purchaseItemRepository = purchaseItemRepository;
        this.purchaseReturnRepository = purchaseReturnRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public Purchase createPurchase(PurchaseRequest request) {
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new RuntimeException("Supplier not found with id: " + request.getSupplierId()));

        Purchase purchase = new Purchase();
        purchase.setSupplier(supplier);
        purchase.setInvoiceNumber(request.getInvoiceNumber());
        purchase.setStatus(request.getStatus() != null ? request.getStatus() : "RECEIVED");
        purchase.setPurchaseDate(LocalDateTime.now());

        // Save initial empty total to generate ID
        purchase.setTotalAmount(0.0);
        Purchase savedPurchase = purchaseRepository.save(purchase);

        double totalAmount = 0.0;
        List<PurchaseItem> items = new ArrayList<>();

        for (PurchaseItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found with id: " + itemReq.getProductId()));

            PurchaseItem item = new PurchaseItem();
            item.setPurchase(savedPurchase);
            item.setProduct(product);
            item.setQuantity(itemReq.getQuantity());
            item.setPrice(itemReq.getPrice());
            purchaseItemRepository.save(item);

            // Increment Stock
            product.setStock(product.getStock() + itemReq.getQuantity());
            productRepository.save(product);

            totalAmount += (itemReq.getQuantity() * itemReq.getPrice());
        }

        savedPurchase.setTotalAmount(totalAmount);
        return purchaseRepository.save(savedPurchase);
    }

    @Transactional
    public PurchaseReturn createPurchaseReturn(Long purchaseId, PurchaseReturnRequest request) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new RuntimeException("Purchase not found with id: " + purchaseId));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + request.getProductId()));

        // Subtract Stock
        product.setStock(product.getStock() - request.getQuantityReturned());
        productRepository.save(product);

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
                .orElseThrow(() -> new RuntimeException("Purchase not found with id: " + id));
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
