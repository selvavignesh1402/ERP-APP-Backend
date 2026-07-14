package com.riceerp.backend.repository;

import com.riceerp.backend.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId);
}
