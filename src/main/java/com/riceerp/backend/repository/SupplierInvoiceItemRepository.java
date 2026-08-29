package com.riceerp.backend.repository;

import com.riceerp.backend.entity.SupplierInvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierInvoiceItemRepository extends JpaRepository<SupplierInvoiceItem, Long> {
    List<SupplierInvoiceItem> findByInvoiceId(Long invoiceId);
}