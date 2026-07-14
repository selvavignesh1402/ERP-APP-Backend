package com.riceerp.backend.repository;

import com.riceerp.backend.entity.PurchaseReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PurchaseReturnRepository extends JpaRepository<PurchaseReturn, Long> {
    List<PurchaseReturn> findByPurchaseId(Long purchaseId);
}
