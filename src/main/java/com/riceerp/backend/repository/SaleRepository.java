package com.riceerp.backend.repository;

import com.riceerp.backend.entity.Sale;
import com.riceerp.backend.enums.PaymentMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {
    @Query("select o.name from Organization o where o.id=:orgId")
    java.util.Optional<String> findShopName(@Param("orgId") Long orgId);

    java.util.Optional<Sale> findFirstByCustomerIdOrderBySaleDateDescIdDesc(Long customerId);
    List<Sale> findByOrganizationId(Long organizationId);
    List<Sale> findBySalesOrderIdAndOrganizationId(Long salesOrderId, Long organizationId);
    List<Sale> findByCustomerIdAndOrganizationIdAndPaymentModeOrderBySaleDateAscIdAsc(
            Long customerId, Long organizationId, PaymentMode paymentMode);
    java.util.Optional<Sale> findByIdAndOrganizationId(Long id, Long organizationId);

    List<Sale> findBySaleDateBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COALESCE(SUM(s.grandTotal), 0) FROM Sale s WHERE s.saleDate BETWEEN :start AND :end")
    BigDecimal sumGrandTotalBySaleDateBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(s.grandTotal), 0) FROM Sale s WHERE s.paymentMode = :mode")
    BigDecimal sumGrandTotalByPaymentMode(@Param("mode") PaymentMode mode);

    List<Sale> findTop5ByOrderBySaleDateDesc();

    java.util.Optional<Sale> findByClientReferenceId(String clientReferenceId);
}
