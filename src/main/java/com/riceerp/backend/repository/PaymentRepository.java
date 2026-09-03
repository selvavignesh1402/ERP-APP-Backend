package com.riceerp.backend.repository;

import com.riceerp.backend.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.id IN :ids")
    double sumAmountByIdIn(@Param("ids") List<Long> ids);
}
