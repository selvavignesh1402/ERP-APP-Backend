package com.riceerp.backend.repository;
import java.math.BigDecimal;

import com.riceerp.backend.entity.Customer;
import com.riceerp.backend.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    java.util.Optional<Customer> findByIdAndOrganizationId(Long id, Long organizationId);
    List<Customer> findByStatus(Status status);

    List<Customer> findByCustomerNameContainingIgnoreCase(String name);

    List<Customer> findByPhoneContaining(String phone);

    @Query("SELECT COALESCE(SUM(c.creditBalance), 0) FROM Customer c WHERE c.status = 'ACTIVE' AND (:orgId IS NULL OR c.organizationId = :orgId)")
    BigDecimal sumActiveCreditBalanceByOrganizationId(@Param("orgId") Long orgId);

    @Query("SELECT COALESCE(SUM(c.creditBalance), 0) FROM Customer c WHERE c.status = 'ACTIVE'")
    BigDecimal sumActiveCreditBalance();
}
