package com.riceerp.backend.repository;

import com.riceerp.backend.entity.SalesOrder;
import com.riceerp.backend.enums.SalesOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {

    Optional<SalesOrder> findByOrderNumber(String orderNumber);

    List<SalesOrder> findByCustomerIdOrderByOrderDateDesc(Long customerId);

    List<SalesOrder> findBySalespersonIdOrderByOrderDateDesc(Long salespersonId);

    List<SalesOrder> findByStatusOrderByOrderDateDesc(SalesOrderStatus status);

    List<SalesOrder> findAllByOrderByOrderDateDesc();

    @Query("SELECT COUNT(s) FROM SalesOrder s WHERE s.status = :status")
    long countByStatus(SalesOrderStatus status);
}
