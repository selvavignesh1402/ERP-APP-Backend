package com.riceerp.backend.repository;

import com.riceerp.backend.entity.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    List<Purchase> findBySupplierId(Long supplierId);

    List<Purchase> findByInvoiceNumberContainingIgnoreCase(String invoiceNumber);

    @Query("SELECT COALESCE(SUM(p.totalAmount), 0) FROM Purchase p WHERE p.purchaseDate BETWEEN :start AND :end")
    double sumTotalAmountByPurchaseDateBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}