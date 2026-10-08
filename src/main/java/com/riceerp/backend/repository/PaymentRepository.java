package com.riceerp.backend.repository;

import com.riceerp.backend.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.math.BigDecimal;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.referenceType = :type AND p.referenceId = :referenceId")
    BigDecimal sumByReference(@Param("type") com.riceerp.backend.enums.ReferenceType type, @Param("referenceId") Long referenceId);
    java.util.Optional<Payment> findByIdAndOrganizationId(Long id, Long organizationId);
    List<Payment> findByReferenceTypeAndReferenceId(com.riceerp.backend.enums.ReferenceType referenceType, Long referenceId);
    java.util.Optional<Payment> findByOrganizationIdAndClientReferenceId(Long organizationId, String clientReferenceId);

    @Query("SELECT COALESCE(SUM(a), 0) FROM Payment p JOIN p.saleAllocations a WHERE KEY(a) = :saleId")
    BigDecimal sumAllocatedToSale(@Param("saleId") Long saleId);

    @Query("SELECT DISTINCT p FROM Payment p LEFT JOIN p.saleAllocations a WHERE " +
            "(p.referenceType = com.riceerp.backend.enums.ReferenceType.SALE AND p.referenceId = :saleId) OR KEY(a) = :saleId")
    List<Payment> findPaymentsForSale(@Param("saleId") Long saleId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.id IN :ids")
    BigDecimal sumAmountByIdIn(@Param("ids") List<Long> ids);
}
