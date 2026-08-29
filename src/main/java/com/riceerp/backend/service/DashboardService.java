package com.riceerp.backend.service;

import com.riceerp.backend.entity.Sale;
import com.riceerp.backend.enums.PaymentMode;
import com.riceerp.backend.enums.Status;
import com.riceerp.backend.repository.ProductRepository;
import com.riceerp.backend.repository.SaleItemRepository;
import com.riceerp.backend.repository.SaleRepository;
import com.riceerp.backend.repository.SupplierRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final SupplierRepository supplierRepository;

    public DashboardService(ProductRepository productRepository,
            SaleRepository saleRepository,
            SaleItemRepository saleItemRepository,
            SupplierRepository supplierRepository) {
        this.productRepository = productRepository;
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.supplierRepository = supplierRepository;
    }

    public Map<String, Object> getDashboardMetrics() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        Map<String, Object> metrics = new HashMap<>();

        // Stock metrics
        metrics.put("totalStock", productRepository.sumStockByActiveStatus());
        metrics.put("lowStockCount", productRepository.countLowStock());

        // Today's sales metrics
        metrics.put("todaySalesKg", saleItemRepository.sumQuantityBySaleDateBetween(startOfDay, endOfDay));
        metrics.put("todayRevenue", saleRepository.sumGrandTotalBySaleDateBetween(startOfDay, endOfDay));

        // Credit pending
        metrics.put("pendingCredit", saleRepository.sumGrandTotalByPaymentMode(PaymentMode.CREDIT));

        // Supplier count
        metrics.put("activeSuppliers", supplierRepository.countByStatus(Status.ACTIVE));

        // Recent 5 sales
        List<Sale> recentSales = saleRepository.findTop5ByOrderBySaleDateDesc();
        List<Map<String, Object>> recentSalesList = recentSales.stream().map(sale -> {
            Map<String, Object> s = new HashMap<>();
            s.put("id", sale.getId());
            s.put("billNumber", sale.getBillNumber());
            s.put("customerName", sale.getCustomerName());
            s.put("grandTotal", sale.getGrandTotal());
            s.put("paymentMode", sale.getPaymentMode().name());
            s.put("saleDate", sale.getSaleDate().toString());
            return s;
        }).collect(Collectors.toList());
        metrics.put("recentSales", recentSalesList);

        return metrics;
    }
}
