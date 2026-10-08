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
import org.springframework.transaction.annotation.Isolation;
import com.riceerp.backend.exception.BusinessRuleException;

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
    private final ProcurementLock procurementLock;

    public SupplierInvoiceService(SupplierInvoiceRepository invoiceRepository,
                                  SupplierInvoiceItemRepository invoiceItemRepository,
                                  SupplierRepository supplierRepository,
                                  PurchaseRepository purchaseRepository,
                                  ProductRepository productRepository, ProcurementLock procurementLock) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.supplierRepository = supplierRepository;
        this.purchaseRepository = purchaseRepository;
        this.productRepository = productRepository;
        this.procurementLock = procurementLock;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SupplierInvoice createInvoice(SupplierInvoiceRequest request) {
        Long orgId = procurementLock.acquire();
        if (request.getInvoiceNumber() == null || request.getInvoiceNumber().trim().isEmpty()) {
            throw new com.riceerp.backend.exception.BusinessRuleException("Invoice number is required.");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new com.riceerp.backend.exception.BusinessRuleException("Invoice must have at least one item.");
        }

        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new com.riceerp.backend.exception.BusinessRuleException("Supplier not found with id: " + request.getSupplierId()));

        Purchase purchase = null;
        if (request.getPurchaseId() != null) {
            purchase = purchaseRepository.findByIdAndOrganizationId(request.getPurchaseId(), orgId)
                    .orElseThrow(() -> new com.riceerp.backend.exception.BusinessRuleException("Purchase not found with id: " + request.getPurchaseId()));
            if (purchase.getSupplier() == null || !java.util.Objects.equals(purchase.getSupplier().getId(), supplier.getId()))
                throw new BusinessRuleException("Invoice supplier does not match the purchase supplier.");
        }

        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setInvoiceNumber(request.getInvoiceNumber());
        invoice.setSupplier(supplier);
        invoice.setPurchase(purchase);
        if (request.getInvoiceDate() != null) {
            invoice.setInvoiceDate(request.getInvoiceDate());
        }
        invoice.setStatus(InvoiceStatus.RECEIVED);
        List<SupplierInvoiceItem> items = new ArrayList<>();
        java.math.BigDecimal maximum = new java.math.BigDecimal("999999999999999.9999");
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        for (SupplierInvoiceItemRequest itemReq : request.getItems()) {
            if (itemReq == null || itemReq.getProductId() == null)
                throw new BusinessRuleException("Each invoice item must specify a product.");
            java.math.BigDecimal quantity = PurchaseQuantities.positive(itemReq.getQuantity());
            java.math.BigDecimal price = itemReq.getUnitPrice();
            if (quantity.compareTo(new java.math.BigDecimal("9999999999999.999999")) > 0
                    || quantity.stripTrailingZeros().scale() > 6)
                throw new BusinessRuleException("Invoice quantity is too large or has more than six decimal places.");
            if (price == null || price.signum() <= 0 || price.compareTo(maximum) > 0
                    || price.stripTrailingZeros().scale() > 4)
                throw new BusinessRuleException("Invoice price must be positive, within the supported range, and have at most four decimal places.");
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new com.riceerp.backend.exception.BusinessRuleException("Product not found with id: " + itemReq.getProductId()));
            SupplierInvoiceItem item = new SupplierInvoiceItem();
            item.setProduct(product);
            item.setQuantity(itemReq.getQuantity());
            item.setUnitPrice(price);
            java.math.BigDecimal lineTotal = quantity.multiply(price).setScale(4, java.math.RoundingMode.HALF_UP);
            item.setTotalAmount(lineTotal);
            items.add(item);
            total = total.add(lineTotal);
            if (total.compareTo(maximum) > 0)
                throw new BusinessRuleException("Invoice total exceeds the supported amount.");
        }
        invoice.setTotalAmount(total);
        SupplierInvoice saved = invoiceRepository.save(invoice);
        for (SupplierInvoiceItem item : items) {
            item.setInvoice(saved);
            invoiceItemRepository.save(item);
        }
        return saved;
    }

    public List<SupplierInvoice> listInvoices(Long supplierId) {
        if (supplierId != null) {
            return invoiceRepository.findBySupplierId(supplierId);
        }
        return invoiceRepository.findAll();
    }

    public SupplierInvoice getInvoiceById(Long id) {
        return invoiceRepository.findByIdAndOrganizationId(id, procurementLock.tenant())
                .orElseThrow(() -> new com.riceerp.backend.exception.BusinessRuleException("Invoice not found with id: " + id));
    }

    public List<SupplierInvoice> getInvoicesForPurchase(Long purchaseId) {
        return invoiceRepository.findByPurchaseIdAndOrganizationId(purchaseId, procurementLock.tenant());
    }

    public List<SupplierInvoiceItem> getInvoiceItems(Long invoiceId) {
        getInvoiceById(invoiceId);
        return invoiceItemRepository.findByInvoiceId(invoiceId);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SupplierInvoice updateStatus(Long id, String status) {
        procurementLock.acquire();
        InvoiceStatus newStatus;
        try {
            newStatus = InvoiceStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new com.riceerp.backend.exception.BusinessRuleException("Invalid invoice status: " + status);
        }
        SupplierInvoice invoice = getInvoiceById(id);
        if (newStatus == InvoiceStatus.MATCHED || newStatus == InvoiceStatus.MISMATCHED)
            throw new BusinessRuleException("Use reconciliation to determine whether this invoice matches.");
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

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SupplierInvoice save(SupplierInvoice invoice) {
        procurementLock.acquire();
        return invoiceRepository.save(invoice);
    }
}
