package com.riceerp.backend.service;

import com.riceerp.backend.dto.OfflineSaleSyncRequest;
import com.riceerp.backend.dto.ProductSalesHistoryResponse;
import com.riceerp.backend.dto.SaleItemRequest;
import com.riceerp.backend.dto.SaleRequest;
import com.riceerp.backend.dto.SyncBatchResponse;
import com.riceerp.backend.entity.Customer;
import com.riceerp.backend.exception.BusinessRuleException;
import com.riceerp.backend.exception.NotFoundException;
import com.riceerp.backend.repository.CustomerRepository;
import com.riceerp.backend.entity.Payment;
import com.riceerp.backend.entity.Product;
import com.riceerp.backend.entity.Sale;
import com.riceerp.backend.entity.SaleItem;
import com.riceerp.backend.enums.MovementType;
import com.riceerp.backend.enums.PaymentMode;
import com.riceerp.backend.enums.ReferenceType;
import com.riceerp.backend.repository.PaymentRepository;
import com.riceerp.backend.repository.ProductRepository;
import com.riceerp.backend.repository.SaleItemRepository;
import com.riceerp.backend.repository.SaleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

@Service
public class SaleService {

    private static final Logger log = LoggerFactory.getLogger(SaleService.class);

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;
    private final StockMovementService stockMovementService;

    public SaleService(SaleRepository saleRepository,
            SaleItemRepository saleItemRepository,
            ProductRepository productRepository,
            PaymentRepository paymentRepository,
            CustomerRepository customerRepository,
            StockMovementService stockMovementService) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.productRepository = productRepository;
        this.paymentRepository = paymentRepository;
        this.customerRepository = customerRepository;
        this.stockMovementService = stockMovementService;
    }

    @Transactional
    public Sale createSale(SaleRequest request) {
        return createSaleInternal(request, null, null);
    }

    @Transactional
    public Sale createSaleInternal(SaleRequest request, String clientReferenceId, LocalDateTime saleDate) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessRuleException("Sale must contain at least one item.");
        }

        // Idempotency check: If sale with clientReferenceId already exists, return existing
        if (clientReferenceId != null && !clientReferenceId.trim().isEmpty()) {
            Optional<Sale> existing = saleRepository.findByClientReferenceId(clientReferenceId.trim());
            if (existing.isPresent()) {
                log.info("Sale with clientReferenceId {} already exists. Skipping duplicate.", clientReferenceId);
                return existing.get();
            }
        }

        Customer customer = null;
        if (request.getCustomerId() != null) {
            customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new NotFoundException("Customer not found with id: " + request.getCustomerId()));
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

        // Credit limit validation logic
        if (PaymentMode.CREDIT.name().equalsIgnoreCase(request.getPaymentMode())) {
            if (customer == null) {
                throw new BusinessRuleException("Customer lookup/registration is required for CREDIT payment sales.");
            }
            if (customer.getCreditLimit() > 0 && customer.getCreditBalance() + grandTotal > customer.getCreditLimit()) {
                throw new BusinessRuleException("Credit limit exceeded! Customer's remaining credit: "
                        + (customer.getCreditLimit() - customer.getCreditBalance()));
            }
            customer.setCreditBalance(customer.getCreditBalance() + grandTotal);
            customerRepository.save(customer);
        }

        // Create Sale Entity
        Sale sale = new Sale();
        sale.setBillNumber("BILL-" + System.currentTimeMillis());
        sale.setCustomerName(customer != null ? customer.getCustomerName() : request.getCustomerName());
        sale.setCustomer(customer);
        sale.setPaymentMode(PaymentMode.valueOf(request.getPaymentMode().toUpperCase()));
        sale.setTotal(totalAmt);
        sale.setDiscount(request.getDiscount());
        sale.setCgst(cgst);
        sale.setSgst(sgst);
        sale.setIgst(0.0);
        sale.setGrandTotal(grandTotal);
        sale.setClientReferenceId(clientReferenceId);
        sale.setSaleDate(saleDate != null ? saleDate : LocalDateTime.now());
        sale.setCreatedAt(LocalDateTime.now());

        Sale savedSale = saleRepository.save(sale);

        // Process line items & deduct stock
        for (SaleItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new NotFoundException("Product not found with id: " + itemReq.getProductId()));

            // Stock Validation
            if (product.getStock() < itemReq.getQuantity()) {
                throw new BusinessRuleException("Insufficient stock for product: " + product.getProductName() +
                        " (Available: " + product.getStock() + ", Requested: " + itemReq.getQuantity() + ")");
            }

            // Deduct Stock
            product.setStock(product.getStock() - itemReq.getQuantity());
            productRepository.save(product);

            // Stock movement ledger
            stockMovementService.record(product, MovementType.SALE, -itemReq.getQuantity(), savedSale.getId());

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
            payment.setPaymentDate(saleDate != null ? saleDate : LocalDateTime.now());
            paymentRepository.save(payment);
        }

        return savedSale;
    }

    // Batch Synchronization of Offline Invoices
    public SyncBatchResponse syncBatchSales(List<OfflineSaleSyncRequest> requests) {
        SyncBatchResponse response = new SyncBatchResponse();
        if (requests == null || requests.isEmpty()) {
            return response;
        }

        response.setTotalProcessed(requests.size());

        for (OfflineSaleSyncRequest req : requests) {
            String clientRef = req.getClientReferenceId();
            try {
                // Check if already synced (Idempotency)
                if (clientRef != null && !clientRef.trim().isEmpty()) {
                    Optional<Sale> existing = saleRepository.findByClientReferenceId(clientRef.trim());
                    if (existing.isPresent()) {
                        Sale s = existing.get();
                        response.getResults().add(new SyncBatchResponse.SyncItemResult(
                                clientRef, s.getId(), s.getBillNumber(), "ALREADY_SYNCED", null));
                        response.setDuplicateCount(response.getDuplicateCount() + 1);
                        continue;
                    }
                }

                // Map to SaleRequest
                SaleRequest saleReq = new SaleRequest();
                saleReq.setCustomerId(req.getCustomerId());
                saleReq.setCustomerName(req.getCustomerName());
                saleReq.setPaymentMode(req.getPaymentMode());
                saleReq.setDiscount(req.getDiscount());
                saleReq.setItems(req.getItems());

                Sale created = createSaleInternal(saleReq, clientRef, req.getOfflineCreatedAt());
                response.getResults().add(new SyncBatchResponse.SyncItemResult(
                        clientRef, created.getId(), created.getBillNumber(), "SYNCED", null));
                response.setSuccessCount(response.getSuccessCount() + 1);

            } catch (Exception ex) {
                log.error("Failed to sync offline sale with clientRef {}: {}", clientRef, ex.getMessage());
                response.getResults().add(new SyncBatchResponse.SyncItemResult(
                        clientRef, null, null, "FAILED", ex.getMessage()));
                response.setFailureCount(response.getFailureCount() + 1);
            }
        }

        return response;
    }

    public List<Sale> listSales() {
        return saleRepository.findAll();
    }

    public Sale getSaleById(Long id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Sale invoice not found with id: " + id));
    }

    public List<SaleItem> getSaleItems(Long saleId) {
        // Ensure sale exists
        getSaleById(saleId);
        return saleItemRepository.findBySaleId(saleId);
    }

    public ProductSalesHistoryResponse getProductSalesHistory(Long productId, LocalDate startDate, LocalDate endDate) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + productId));

        LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        if (start.isAfter(end)) {
            throw new BusinessRuleException("Start date cannot be after end date.");
        }

        LocalDateTime startTime = start.atStartOfDay();
        LocalDateTime endTime = end.atTime(LocalTime.MAX);

        double totalQuantity = saleItemRepository.sumQuantityByProductIdAndSaleDateBetween(productId, startTime, endTime);
        double totalRevenue = saleItemRepository.sumRevenueByProductIdAndSaleDateBetween(productId, startTime, endTime);
        long salesCount = saleItemRepository.countSalesByProductIdAndSaleDateBetween(productId, startTime, endTime);

        List<SaleItem> items = saleItemRepository
                .findByProductIdAndSaleDateBetweenOrderBySaleDateDesc(productId, startTime, endTime);

        Map<LocalDate, double[]> daily = new TreeMap<>();
        for (SaleItem item : items) {
            LocalDate date = item.getSale().getSaleDate().toLocalDate();
            double[] acc = daily.computeIfAbsent(date, d -> new double[2]);
            acc[0] += item.getQuantity();
            acc[1] += item.getQuantity() * item.getPrice();
        }

        long days = end.toEpochDay() - start.toEpochDay() + 1;

        ProductSalesHistoryResponse resp = new ProductSalesHistoryResponse();
        resp.setProductId(product.getId());
        resp.setProductName(product.getProductName());
        resp.setUnit(product.getUnit());
        resp.setStartDate(start);
        resp.setEndDate(end);
        resp.setTotalQuantitySold(Math.round(totalQuantity * 100.0) / 100.0);
        resp.setTotalRevenue(Math.round(totalRevenue * 100.0) / 100.0);
        resp.setSalesCount(salesCount);
        resp.setAverageDailySales(Math.round((totalQuantity / days) * 100.0) / 100.0);
        daily.forEach((date, acc) -> resp.getBreakdown().add(
                new ProductSalesHistoryResponse.DailyBreakdown(date,
                        Math.round(acc[0] * 100.0) / 100.0,
                        Math.round(acc[1] * 100.0) / 100.0)));
        return resp;
    }

    @Transactional
    public Sale createSaleFromDelivery(Long deliveryId, Long salesOrderId, Customer customer, List<SaleItemRequest> items, PaymentMode paymentMode, double discount) {
        SaleRequest request = new SaleRequest();
        request.setCustomerId(customer != null ? customer.getId() : null);
        request.setCustomerName(customer != null ? customer.getCustomerName() : "Counter Sale");
        request.setPaymentMode(paymentMode != null ? paymentMode.name() : PaymentMode.CREDIT.name());
        request.setDiscount(discount);
        request.setItems(items);

        String clientRef = "DELIVERY-" + deliveryId + "-" + System.currentTimeMillis();
        Sale sale = createSaleInternal(request, clientRef, LocalDateTime.now());
        sale.setDeliveryId(deliveryId);
        sale.setSalesOrderId(salesOrderId);
        return saleRepository.save(sale);
    }
}
