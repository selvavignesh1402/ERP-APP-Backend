package com.riceerp.backend.service;

import com.riceerp.backend.dto.SaleItemRequest;
import com.riceerp.backend.dto.SaleRequest;
import com.riceerp.backend.entity.Payment;
import com.riceerp.backend.entity.Product;
import com.riceerp.backend.entity.Sale;
import com.riceerp.backend.entity.SaleItem;
import com.riceerp.backend.enums.PaymentMode;
import com.riceerp.backend.enums.ReferenceType;
import com.riceerp.backend.repository.PaymentRepository;
import com.riceerp.backend.repository.ProductRepository;
import com.riceerp.backend.repository.SaleItemRepository;
import com.riceerp.backend.repository.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;

    public SaleService(SaleRepository saleRepository,
            SaleItemRepository saleItemRepository,
            ProductRepository productRepository,
            PaymentRepository paymentRepository) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.productRepository = productRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public Sale createSale(SaleRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("Sale must contain at least one item.");
        }

        // Calculate Subtotal
        double totalAmt = 0.0;
        for (SaleItemRequest itemReq : request.getItems()) {
            totalAmt += itemReq.getQuantity() * itemReq.getPrice();
        }

        double netTotal = totalAmt - request.getDiscount();
        if (netTotal < 0)
            netTotal = 0;

        // Taxes structures (CGST 2.5%, SGST 2.5%)
        double cgst = netTotal * 0.025;
        double sgst = netTotal * 0.025;
        double grandTotal = netTotal + cgst + sgst;

        // Create Sale Entity
        Sale sale = new Sale();
        sale.setBillNumber("BILL-" + System.currentTimeMillis());
        sale.setCustomerName(request.getCustomerName());
        sale.setPaymentMode(PaymentMode.valueOf(request.getPaymentMode().toUpperCase()));
        sale.setTotal(totalAmt);
        sale.setDiscount(request.getDiscount());
        sale.setCgst(cgst);
        sale.setSgst(sgst);
        sale.setIgst(0.0);
        sale.setGrandTotal(grandTotal);
        sale.setSaleDate(LocalDateTime.now());
        sale.setCreatedAt(LocalDateTime.now());

        Sale savedSale = saleRepository.save(sale);

        // Process line items & deduct stock
        for (SaleItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found with id: " + itemReq.getProductId()));

            // Stock Validation
            if (product.getStock() < itemReq.getQuantity()) {
                throw new RuntimeException("Insufficient stock for product: " + product.getProductName() +
                        " (Available: " + product.getStock() + ", Requested: " + itemReq.getQuantity() + ")");
            }

            // Deduct Stock
            product.setStock(product.getStock() - itemReq.getQuantity());
            productRepository.save(product);

            // Save SaleItem
            SaleItem saleItem = new SaleItem();
            saleItem.setSale(savedSale);
            saleItem.setProduct(product);
            saleItem.setQuantity(itemReq.getQuantity());
            saleItem.setPrice(itemReq.getPrice());
            saleItemRepository.save(saleItem);
        }

        // Auto-payment integration
        if (!PaymentMode.CREDIT.name().equalsIgnoreCase(request.getPaymentMode())) {
            Payment payment = new Payment();
            payment.setReferenceType(ReferenceType.SALE);
            payment.setReferenceId(savedSale.getId());
            payment.setAmount(grandTotal);
            payment.setPaymentMode(PaymentMode.valueOf(request.getPaymentMode().toUpperCase()));
            payment.setPaymentDate(LocalDateTime.now());
            paymentRepository.save(payment);
        }

        return savedSale;
    }

    public List<Sale> listSales() {
        return saleRepository.findAll();
    }

    public Sale getSaleById(Long id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sale invoice not found with id: " + id));
    }

    public List<SaleItem> getSaleItems(Long saleId) {
        // Ensure sale exists
        getSaleById(saleId);
        return saleItemRepository.findBySaleId(saleId);
    }
}
