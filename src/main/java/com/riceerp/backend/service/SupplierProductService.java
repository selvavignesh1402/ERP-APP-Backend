package com.riceerp.backend.service;

import com.riceerp.backend.dto.SupplierOptionResponse;
import com.riceerp.backend.dto.SupplierProductRequest;
import com.riceerp.backend.entity.Product;
import com.riceerp.backend.entity.Supplier;
import com.riceerp.backend.entity.SupplierProduct;
import com.riceerp.backend.repository.ProductRepository;
import com.riceerp.backend.repository.SupplierProductRepository;
import com.riceerp.backend.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SupplierProductService {
    private static void validatePrice(SupplierProductRequest request) {
        var price = request.getPurchasePrice();
        if (price == null || price.signum() <= 0 || price.stripTrailingZeros().scale() > 4 ||
                price.compareTo(new java.math.BigDecimal("999999999999999.9999")) > 0)
            throw new com.riceerp.backend.exception.BusinessRuleException("Supplier purchase price must be positive, within the supported range, and have at most four decimal places.");
    }

    private final SupplierProductRepository supplierProductRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;

    public SupplierProductService(SupplierProductRepository supplierProductRepository,
                                  SupplierRepository supplierRepository,
                                  ProductRepository productRepository) {
        this.supplierProductRepository = supplierProductRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public SupplierProduct assignProduct(Long supplierId, SupplierProductRequest request) {
        validatePrice(request);
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new com.riceerp.backend.exception.BusinessRuleException("Supplier not found with id: " + supplierId));
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new com.riceerp.backend.exception.BusinessRuleException("Product not found with id: " + request.getProductId()));

        if (supplierProductRepository.findBySupplierIdAndProductId(supplierId, request.getProductId()).isPresent()) {
            throw new com.riceerp.backend.exception.BusinessRuleException("Product is already assigned to this supplier.");
        }

        SupplierProduct sp = new SupplierProduct();
        sp.setSupplier(supplier);
        sp.setProduct(product);
        sp.setPurchasePrice(request.getPurchasePrice());
        sp.setLeadTimeDays(request.getLeadTimeDays());
        sp.setMinOrderQty(request.getMinOrderQty());
        return supplierProductRepository.save(sp);
    }

    @Transactional
    public SupplierProduct updateProcurementData(Long supplierId, Long productId, SupplierProductRequest request) {
        validatePrice(request);
        SupplierProduct sp = supplierProductRepository.findBySupplierIdAndProductId(supplierId, productId)
                .orElseThrow(() -> new com.riceerp.backend.exception.BusinessRuleException("Product is not assigned to this supplier."));
        sp.setPurchasePrice(request.getPurchasePrice());
        sp.setLeadTimeDays(request.getLeadTimeDays());
        sp.setMinOrderQty(request.getMinOrderQty());
        return supplierProductRepository.save(sp);
    }

    public List<SupplierProduct> listSupplierProducts(Long supplierId) {
        return supplierProductRepository.findBySupplierId(supplierId);
    }

    public List<SupplierOptionResponse> getSupplierOptionsForProduct(Long productId) {
        // Ensure product exists
        productRepository.findById(productId)
                .orElseThrow(() -> new com.riceerp.backend.exception.BusinessRuleException("Product not found with id: " + productId));

        return supplierProductRepository.findByProductId(productId).stream()
                .map(sp -> {
                    SupplierOptionResponse resp = new SupplierOptionResponse();
                    resp.setSupplierId(sp.getSupplier().getId());
                    resp.setSupplierName(sp.getSupplier().getSupplierName());
                    resp.setPurchasePrice(sp.getPurchasePrice());
                    resp.setLeadTimeDays(sp.getLeadTimeDays());
                    resp.setMinOrderQty(sp.getMinOrderQty());
                    return resp;
                })
                .sorted(Comparator.comparing(SupplierOptionResponse::getPurchasePrice))
                .collect(Collectors.toList());
    }
}
