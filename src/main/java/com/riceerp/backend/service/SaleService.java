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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class SaleService {

    private static final Logger log = LoggerFactory.getLogger(SaleService.class);
    private static final BigDecimal MAX_STORED_AMOUNT = new BigDecimal("999999999999999.9999");

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;
    private final StockMovementService stockMovementService;
    private final TransactionTemplate transactionTemplate;

    public SaleService(SaleRepository saleRepository,
            SaleItemRepository saleItemRepository,
            ProductRepository productRepository,
            PaymentRepository paymentRepository,
            CustomerRepository customerRepository,
            StockMovementService stockMovementService,
            PlatformTransactionManager transactionManager) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.productRepository = productRepository;
        this.paymentRepository = paymentRepository;
        this.customerRepository = customerRepository;
        this.stockMovementService = stockMovementService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public Sale createSale(SaleRequest request) {
        return createSaleInternal(request, null, null);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public Sale createSaleInternal(SaleRequest request, String clientReferenceId, LocalDateTime saleDate) {
        return createSaleWithRates(request, clientReferenceId, saleDate, Map.of());
    }

    private Sale createSaleWithRates(SaleRequest request, String clientReferenceId, LocalDateTime saleDate,
                                     Map<Long, Double> rateSnapshots) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessRuleException("Sale must contain at least one item.");
        }

        // Idempotency check: If sale with clientReferenceId already exists, return existing
        if (clientReferenceId != null && !clientReferenceId.trim().isEmpty()) {
            Optional<Sale> existing = saleRepository.findByClientReferenceId(clientReferenceId.trim());
            if (existing.isPresent()) {
                log.info("Sale with clientReferenceId {} already exists. Skipping duplicate.", clientReferenceId);
                return withPaymentTotals(existing.get());
            }
        }

        Customer customer = null;
        if (request.getCustomerId() != null) {
            customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new NotFoundException("Customer not found with id: " + request.getCustomerId()));
        }

        // Validate line items, fetch and verify products and prices
        Map<Long, Product> productsToUpdate = new LinkedHashMap<>();
        Map<Long, BigDecimal> requestedQuantities = new LinkedHashMap<>();
        BigDecimal totalValue = BigDecimal.ZERO;
        List<GstCalculator.Line> taxLines = new java.util.ArrayList<>();

        for (SaleItemRequest itemReq : request.getItems()) {
            if (itemReq == null || itemReq.getProductId() == null) {
                throw new BusinessRuleException("Each sale item must specify a product.");
            }
            if (!Double.isFinite(itemReq.getQuantity()) || itemReq.getQuantity() <= 0) {
                throw new BusinessRuleException("Item quantity must be finite and greater than zero.");
            }

            Product product = productsToUpdate.computeIfAbsent(itemReq.getProductId(), id ->
                    productRepository.findForStockUpdate(id)
                            .orElseThrow(() -> new NotFoundException("Product not found with id: " + id)));
            requestedQuantities.merge(itemReq.getProductId(), BigDecimal.valueOf(itemReq.getQuantity()), BigDecimal::add);

            // Price validation and defaulting
            BigDecimal effectivePrice = itemReq.getPrice();
            if (effectivePrice == null || effectivePrice.signum() < 0)
                throw new BusinessRuleException("Item price must be non-negative and present.");
            if (effectivePrice.signum() == 0) {
                effectivePrice = product.getSellingPrice();
                itemReq.setPrice(effectivePrice);
            } else if (product.getPurchasePrice().signum() > 0 && effectivePrice.compareTo(product.getPurchasePrice()) < 0) {
                throw new BusinessRuleException("Selling price (₹" + effectivePrice + ") cannot be lower than cost price (₹" + product.getPurchasePrice() + ") for " + product.getProductName());
            }

            if (effectivePrice.signum() <= 0 || effectivePrice.compareTo(MAX_STORED_AMOUNT) > 0 || effectivePrice.stripTrailingZeros().scale() > 4)
                throw new BusinessRuleException("Item price must be positive, fit the supported range, and have at most four decimal places.");
            totalValue = totalValue.add(DecimalAmounts.value(itemReq.getQuantity()).multiply(effectivePrice));
            Double rate = rateSnapshots.containsKey(product.getId()) ? rateSnapshots.get(product.getId()) : product.getGstRate();
            if (itemReq.isGstRateProvided() && !java.util.Objects.equals(GstCalculator.rate(itemReq.getGstRate()), GstCalculator.rate(rate)))
                throw new BusinessRuleException("Product GST changed. Refresh this product and review the invoice before submitting.");
            taxLines.add(new GstCalculator.Line(DecimalAmounts.value(itemReq.getQuantity())
                    .multiply(effectivePrice), rate));
        }

        // Validate the combined demand before writing any sale, credit, or stock changes.
        for (Map.Entry<Long, BigDecimal> entry : requestedQuantities.entrySet()) {
            Product product = productsToUpdate.get(entry.getKey());
            if (!Double.isFinite(product.getStock()) ||
                    BigDecimal.valueOf(product.getStock() - productRepository.reservedQuantity(product.getId())).compareTo(entry.getValue()) < 0) {
                throw new BusinessRuleException("Insufficient stock for product: " + product.getProductName() +
                        " (Unreserved available: " + Math.max(0, product.getStock() - productRepository.reservedQuantity(product.getId())) + ", Requested: " + entry.getValue() + ")");
            }
        }

        BigDecimal discountValue = request.getDiscount();
        if (discountValue == null || discountValue.signum() < 0 ||
                discountValue.compareTo(MAX_STORED_AMOUNT) > 0 || discountValue.stripTrailingZeros().scale() > 4) {
            throw new BusinessRuleException("Discount must be zero or greater, with at most 15 integer digits and four decimal places.");
        }
        if (discountValue.compareTo(totalValue) > 0) {
            throw new BusinessRuleException("Discount (₹" + discountValue + ") cannot exceed total sale amount (₹" + totalValue + ").");
        }

        BigDecimal netTotal = totalValue.subtract(discountValue);

        GstCalculator.Totals taxes = GstCalculator.calculate(taxLines, discountValue, request.getTaxType());
        BigDecimal grandTotal = DecimalAmounts.cents(netTotal.add(taxes.total()));
        if (DecimalAmounts.cents(totalValue).compareTo(MAX_STORED_AMOUNT) > 0 ||
                grandTotal.compareTo(MAX_STORED_AMOUNT) > 0) {
            throw new BusinessRuleException("Invoice total exceeds the supported maximum.");
        }

        PaymentMode saleMode;
        try { saleMode = PaymentMode.valueOf(request.getPaymentMode().toUpperCase(java.util.Locale.ROOT)); }
        catch (RuntimeException ex) { throw new BusinessRuleException("Invalid payment mode."); }
        BigDecimal paidAmount = request.getPaidAmount() == null
                ? (saleMode == PaymentMode.CREDIT ? BigDecimal.ZERO : grandTotal)
                : request.getPaidAmount();
        if (paidAmount.signum() < 0 || paidAmount.compareTo(grandTotal) > 0 ||
                paidAmount.stripTrailingZeros().scale() > 2) {
            throw new BusinessRuleException("Paid amount must be between zero and the invoice total, with at most two decimal places.");
        }
        if (saleMode != PaymentMode.CREDIT && paidAmount.compareTo(grandTotal) != 0) {
            throw new BusinessRuleException("Use CREDIT with an initial payment for a partially paid sale.");
        }
        PaymentMode collectionMode = saleMode;
        if (saleMode == PaymentMode.CREDIT && paidAmount.signum() > 0) {
            try { collectionMode = PaymentMode.valueOf(request.getInitialPaymentMode().toUpperCase(java.util.Locale.ROOT)); }
            catch (RuntimeException ex) { throw new BusinessRuleException("Select CASH, UPI or CARD for the initial payment."); }
            if (collectionMode == PaymentMode.CREDIT) throw new BusinessRuleException("Initial payment cannot use CREDIT.");
        }
        BigDecimal balanceDue = grandTotal.subtract(paidAmount);

        // Credit limit validation logic
        if (PaymentMode.CREDIT.name().equalsIgnoreCase(request.getPaymentMode())) {
            if (customer == null) {
                throw new BusinessRuleException("Customer lookup/registration is required for CREDIT payment sales.");
            }
            BigDecimal newBalance = customer.getCreditBalance().add(balanceDue);
            if (customer.getCreditLimit().signum() > 0 && newBalance.compareTo(customer.getCreditLimit()) > 0) {
                throw new BusinessRuleException("Credit limit exceeded! Customer's remaining credit: "
                        + customer.getCreditLimit().subtract(customer.getCreditBalance()).toPlainString());
            }
            customer.setCreditBalance(newBalance);
            customerRepository.save(customer);
        }

        // Create Sale Entity
        Sale sale = new Sale();
        sale.setBillNumber("BILL-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        sale.setCustomerName(customer != null ? customer.getCustomerName() : request.getCustomerName());
        sale.setCustomer(customer);
        sale.setCustomerPhone(customer == null ? null : customer.getPhone());
        sale.setCustomerAddress(customer == null ? null : customer.getAddress());
        sale.setShopName(saleRepository.findShopName(com.riceerp.backend.security.TenantContext.getCurrentTenant()).orElse(null));
        sale.setPaymentMode(PaymentMode.valueOf(request.getPaymentMode().toUpperCase()));
        sale.setTotal(DecimalAmounts.cents(totalValue));
        sale.setDiscount(DecimalAmounts.cents(discountValue));
        sale.setTaxType(request.getTaxType());
        sale.setCgst(taxes.cgst());
        sale.setSgst(taxes.sgst());
        sale.setIgst(taxes.igst());
        sale.setGrandTotal(grandTotal);
        sale.setClientReferenceId(clientReferenceId == null ? null : clientReferenceId.trim());
        sale.setSaleDate(saleDate != null ? saleDate : LocalDateTime.now());
        sale.setCreatedAt(LocalDateTime.now());

        Sale savedSale = saleRepository.save(sale);

        // Deduct once per product; retain separate invoice lines and their prices below.
        for (Map.Entry<Long, BigDecimal> entry : requestedQuantities.entrySet()) {
            Product product = productsToUpdate.get(entry.getKey());
            product.setStock(BigDecimal.valueOf(product.getStock()).subtract(entry.getValue()).doubleValue());
            productRepository.save(product);
            stockMovementService.record(product, MovementType.SALE, -entry.getValue().doubleValue(), savedSale.getId());
        }

        for (SaleItemRequest itemReq : request.getItems()) {
            Product product = productsToUpdate.get(itemReq.getProductId());

            // Save SaleItem
            SaleItem saleItem = new SaleItem();
            saleItem.setSale(savedSale);
            saleItem.setProduct(product);
            saleItem.setProductName(product.getProductName());
            saleItem.setUnit(product.getUnit());
            saleItem.setGstRate(GstCalculator.rate(rateSnapshots.containsKey(product.getId())
                    ? rateSnapshots.get(product.getId()) : product.getGstRate()));
            saleItem.setQuantity(itemReq.getQuantity());
            saleItem.setPrice(itemReq.getPrice());
            saleItemRepository.save(saleItem);
        }

        // Auto-payment integration
        if (paidAmount.signum() > 0) {
            Payment payment = new Payment();
            payment.setReferenceType(ReferenceType.SALE);
            payment.setReferenceId(savedSale.getId());
            payment.setAmount(paidAmount);
            payment.setPaymentMode(collectionMode);
            payment.setPaymentDate(saleDate != null ? saleDate : LocalDateTime.now());
            paymentRepository.save(payment);
        }

        savedSale.setPaidAmount(paidAmount);
        savedSale.setBalanceDue(balanceDue);
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
                if (clientRef == null || clientRef.isBlank() || clientRef.length() > 64) {
                    throw new BusinessRuleException("A valid offline sale reference is required");
                }
                // Check if already synced (Idempotency)
                if (clientRef != null && !clientRef.trim().isEmpty()) {
                    Optional<Sale> existing = saleRepository.findByClientReferenceId(clientRef.trim());
                    if (existing.isPresent()) {
                        Sale s = withPaymentTotals(existing.get());
                        response.getResults().add(new SyncBatchResponse.SyncItemResult(
                                clientRef, s.getId(), s.getBillNumber(), "ALREADY_SYNCED", null).withSale(s));
                        response.setDuplicateCount(response.getDuplicateCount() + 1);
                        continue;
                    }
                }

                // Map to SaleRequest
                SaleRequest saleReq = new SaleRequest();
                saleReq.setCustomerId(req.getCustomerId());
                saleReq.setCustomerName(req.getCustomerName());
                saleReq.setPaymentMode(req.getPaymentMode());
                saleReq.setPaidAmount(req.getPaidAmount());
                saleReq.setInitialPaymentMode(req.getInitialPaymentMode());
                saleReq.setTaxType(req.getTaxType());
                saleReq.setDiscount(req.getDiscount());
                saleReq.setItems(req.getItems());

                Sale created = transactionTemplate.execute(status ->
                        createSaleInternal(saleReq, clientRef, req.getOfflineCreatedAt())
                );
                response.getResults().add(new SyncBatchResponse.SyncItemResult(
                        clientRef, created != null ? created.getId() : null, created != null ? created.getBillNumber() : null, "SYNCED", null).withSale(created));
                response.setSuccessCount(response.getSuccessCount() + 1);

            } catch (Exception ex) {
                // A concurrent sync may have committed this key while our transaction
                // rolled back on uniqueness/version conflict. Return the durable sale.
                if (clientRef != null && !clientRef.isBlank()) {
                    Optional<Sale> committed = saleRepository.findByClientReferenceId(clientRef.trim());
                    if (committed.isPresent()) {
                        Sale existing = withPaymentTotals(committed.get());
                        response.getResults().add(new SyncBatchResponse.SyncItemResult(clientRef,
                                existing.getId(), existing.getBillNumber(), "ALREADY_SYNCED", null).withSale(existing));
                        response.setDuplicateCount(response.getDuplicateCount() + 1);
                        continue;
                    }
                }
                log.error("Failed to sync offline sale with clientRef {}: {}", clientRef, ex.getMessage());
                response.getResults().add(new SyncBatchResponse.SyncItemResult(
                        clientRef, null, null, "FAILED", ex.getMessage()));
                response.setFailureCount(response.getFailureCount() + 1);
            }
        }

        return response;
    }

    public List<Sale> listSales() {
        return saleRepository.findAll().stream().map(this::withPaymentTotals).toList();
    }

    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public com.riceerp.backend.dto.SalesSummary salesSummary() {
        Long orgId = com.riceerp.backend.security.TenantContext.getCurrentTenant();
        if (orgId == null || orgId <= 0) throw new org.springframework.security.access.AccessDeniedException("Select an organization first");
        List<Sale> sales = saleRepository.findByOrganizationId(orgId).stream().map(this::withPaymentTotals).toList();
        return com.riceerp.backend.dto.SalesSummary.fromSales(sales, LocalDateTime.now());
    }

    public Sale getSaleById(Long id) {
        return withPaymentTotals(saleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Sale invoice not found with id: " + id)));
    }

    private Sale withPaymentTotals(Sale sale) {
        BigDecimal paid = DecimalAmounts.cents(paymentRepository.sumByReference(ReferenceType.SALE, sale.getId())
                .add(paymentRepository.sumAllocatedToSale(sale.getId())));
        sale.setPaidAmount(paid);
        sale.setBalanceDue(DecimalAmounts.cents(sale.getGrandTotal().subtract(paid))
                .max(BigDecimal.ZERO));
        return sale;
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

        Map<LocalDate, BigDecimal[]> daily = new TreeMap<>();
        for (SaleItem item : items) {
            LocalDate date = item.getSale().getSaleDate().toLocalDate();
            BigDecimal[] acc = daily.computeIfAbsent(date, d -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            acc[0] = acc[0].add(DecimalAmounts.value(item.getQuantity()));
            acc[1] = acc[1].add(DecimalAmounts.value(item.getQuantity()).multiply(item.getPrice()));
        }

        long days = end.toEpochDay() - start.toEpochDay() + 1;

        ProductSalesHistoryResponse resp = new ProductSalesHistoryResponse();
        resp.setProductId(product.getId());
        resp.setProductName(product.getProductName());
        resp.setUnit(product.getUnit());
        resp.setStartDate(start);
        resp.setEndDate(end);
        resp.setTotalQuantitySold(DecimalAmounts.cents(totalQuantity));
        resp.setTotalRevenue(DecimalAmounts.cents(totalRevenue));
        resp.setSalesCount(salesCount);
        resp.setAverageDailySales(DecimalAmounts.value(totalQuantity).divide(BigDecimal.valueOf(days), 2, java.math.RoundingMode.HALF_UP).doubleValue());
        daily.forEach((date, acc) -> resp.getBreakdown().add(
                new ProductSalesHistoryResponse.DailyBreakdown(date,
                        DecimalAmounts.cents(acc[0]).doubleValue(),
                        DecimalAmounts.cents(acc[1]).doubleValue())));
        return resp;
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public Sale createSaleFromDelivery(Long deliveryId, Long salesOrderId, Customer customer, List<SaleItemRequest> items, PaymentMode paymentMode, double discount) {
        return createSaleFromDelivery(deliveryId, salesOrderId, customer, items, paymentMode, DecimalAmounts.value(discount),
                com.riceerp.backend.enums.TaxType.INTRA_STATE, Map.of());
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public Sale createSaleFromDelivery(Long deliveryId, Long salesOrderId, Customer customer, List<SaleItemRequest> items,
                                      PaymentMode paymentMode, BigDecimal discount, com.riceerp.backend.enums.TaxType taxType,
                                      Map<Long, Double> rateSnapshots) {
        SaleRequest request = new SaleRequest();
        request.setCustomerId(customer != null ? customer.getId() : null);
        request.setCustomerName(customer != null ? customer.getCustomerName() : "Counter Sale");
        request.setPaymentMode(paymentMode != null ? paymentMode.name() : PaymentMode.CREDIT.name());
        request.setDiscount(discount);
        request.setTaxType(taxType);
        request.setItems(items);

        String clientRef = "DELIVERY-" + deliveryId;
        Optional<Sale> previous = saleRepository.findByClientReferenceId(clientRef);
        if (previous.isPresent()) {
            Sale existing = previous.get();
            if (!java.util.Objects.equals(existing.getDeliveryId(), deliveryId) ||
                    !java.util.Objects.equals(existing.getSalesOrderId(), salesOrderId)) {
                throw new BusinessRuleException("Delivery invoice reference conflicts with another sale.");
            }
            return withPaymentTotals(existing);
        }
        Sale sale = createSaleWithRates(request, clientRef, LocalDateTime.now(), rateSnapshots);
        sale.setDeliveryId(deliveryId);
        sale.setSalesOrderId(salesOrderId);
        return saleRepository.save(sale);
    }
}
