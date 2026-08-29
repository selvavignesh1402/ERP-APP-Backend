package com.riceerp.backend.service;

import com.riceerp.backend.dto.SupplierInvoiceItemRequest;
import com.riceerp.backend.dto.SupplierInvoiceRequest;
import com.riceerp.backend.entity.Product;
import com.riceerp.backend.entity.Purchase;
import com.riceerp.backend.entity.Supplier;
import com.riceerp.backend.entity.SupplierInvoice;
import com.riceerp.backend.entity.SupplierInvoiceItem;
import com.riceerp.backend.enums.InvoiceStatus;
import com.riceerp.backend.repository.ProductRepository;
import com.riceerp.backend.repository.PurchaseRepository;
import com.riceerp.backend.repository.SupplierInvoiceItemRepository;
import com.riceerp.backend.repository.SupplierInvoiceRepository;
import com.riceerp.backend.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SupplierInvoiceService {

    private final SupplierInvoiceRepository invoiceRepository;
    private final SupplierInvoiceItemRepository invoiceItemRepository;
    private final SupplierRepository supplierRepository;
    private final PurchaseRepository purchaseRepository;
    private final ProductRepository productRepository;

    public SupplierInvoiceService(SupplierInvoiceRepository invoiceRepository,
                                  SupplierInvoiceItemRepository invoiceItemRepository,
                                  SupplierRepository supplierRepository,
                                  PurchaseRepository purchaseRepository,
                                  ProductRepository productRepository) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.supplierRepository = supplierRepository;
        this.purchaseRepository = purchaseRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public SupplierInvoice createInvoice(SupplierInvoiceRequest request) {
        if (request.getInvoiceNumber() == null || request.getInvoiceNumber().trim().isEmpty()) {
            throw new RuntimeException("Invoice number is required.");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("Invoice must have at least one item.");
        }

        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new RuntimeException("Supplier not found with id: " + request.getSupplierId()));

        Purchase purchase = null;
        if (request.getPurchaseId() != null) {
            purchase = purchaseRepository.findById(request.getPurchaseId())
                    .orElseThrow(() -> new RuntimeException("Purchase not found with id: " + request.getPurchaseId()));
        }

        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setInvoiceNumber(request.getInvoiceNumber());
        invoice.setSupplier(supplier);
        invoice.setPurchase(purchase);
        if (request.getInvoiceDate() != null) {
            invoice.setInvoiceDate(request.getInvoiceDate());
        }
        invoice.setStatus(InvoiceStatus.RECEIVED);
        invoice = invoiceRepository.save(invoice);

        double total = 0.0;
        for (SupplierInvoiceItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found with id: " + itemReq.getProductId()));
            if (itemReq.getQuantity() <= 0 || itemReq.getUnitPrice() < 0) {
                throw new RuntimeException("Invalid quantity or unit price for product: " + product.getProductName());
            }
            SupplierInvoiceItem item = new SupplierInvoiceItem();
            item.setInvoice(invoice);
            item.setProduct(product);
            item.setQuantity(itemReq.getQuantity());
            item.setUnitPrice(itemReq.getUnitPrice());
            item.setTotalAmount(itemReq.getQuantity() * itemReq.getUnitPrice());
            invoiceItemRepository.save(item);
            total += item.getTotalAmount();
        }
        invoice.setTotalAmount(total);
        return invoiceRepository.save(invoice);
    }

    public List<SupplierInvoice> listInvoices(Long supplierId) {
        if (supplierId != null) {
            return invoiceRepository.findBySupplierId(supplierId);
        }
        return invoiceRepository.findAll();
    }

    public SupplierInvoice getInvoiceById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));
    }

    public List<SupplierInvoice> getInvoicesForPurchase(Long purchaseId) {
        return invoiceRepository.findByPurchaseId(purchaseId);
    }

    public List<SupplierInvoiceItem> getInvoiceItems(Long invoiceId) {
        getInvoiceById(invoiceId);
        return invoiceItemRepository.findByInvoiceId(invoiceId);
    }

    @Transactional
    public SupplierInvoice updateStatus(Long id, String status) {
        InvoiceStatus newStatus;
        try {
            newStatus = InvoiceStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid invoice status: " + status);
        }
        SupplierInvoice invoice = getInvoiceById(id);
        invoice.setStatus(newStatus);
        return invoiceRepository.save(invoice);
    }

    public List<SupplierInvoiceItem> getInvoiceItemsByPurchase(Long purchaseId) {
        List<SupplierInvoice> invoices = invoiceRepository.findByPurchaseId(purchaseId);
        List<SupplierInvoiceItem> all = new ArrayList<>();
        for (SupplierInvoice inv : invoices) {
            all.addAll(invoiceItemRepository.findByInvoiceId(inv.getId()));
        }
        return all;
    }
}