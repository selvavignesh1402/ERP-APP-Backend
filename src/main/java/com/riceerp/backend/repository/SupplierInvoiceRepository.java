package com.riceerp.backend.repository;

import com.riceerp.backend.entity.SupplierInvoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierInvoiceRepository extends JpaRepository<SupplierInvoice, Long> {
    List<SupplierInvoice> findByPurchaseId(Long purchaseId);
    List<SupplierInvoice> findBySupplierId(Long supplierId);
}