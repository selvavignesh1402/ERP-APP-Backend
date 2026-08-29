package com.riceerp.backend.repository;

import com.riceerp.backend.entity.ReconciliationResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReconciliationResultRepository extends JpaRepository<ReconciliationResult, Long> {
    Optional<ReconciliationResult> findFirstByPurchaseIdAndInvoiceIdOrderByReconciledAtDesc(Long purchaseId, Long invoiceId);
    List<ReconciliationResult> findByPurchaseIdOrderByReconciledAtDesc(Long purchaseId);
}