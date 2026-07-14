package com.riceerp.backend.repository;

import com.riceerp.backend.entity.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    List<Purchase> findBySupplierId(Long supplierId);
    List<Purchase> findByInvoiceNumberContainingIgnoreCase(String invoiceNumber);
}
