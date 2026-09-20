package com.riceerp.backend.repository;

import com.riceerp.backend.entity.Delivery;
import com.riceerp.backend.enums.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    Optional<Delivery> findByDeliveryNumberAndOrganizationId(String deliveryNumber, Long organizationId);

    List<Delivery> findBySalesOrderIdAndOrganizationId(Long salesOrderId, Long organizationId);

    List<Delivery> findByDeliveryPersonIdAndOrganizationIdOrderByAssignedAtDesc(Long deliveryPersonId, Long organizationId);

    List<Delivery> findByDeliveryPersonIdAndStatusAndOrganizationIdOrderByAssignedAtDesc(Long deliveryPersonId, DeliveryStatus status, Long organizationId);

    List<Delivery> findByStatusAndOrganizationIdOrderByAssignedAtDesc(DeliveryStatus status, Long organizationId);

    List<Delivery> findByOrganizationIdOrderByAssignedAtDesc(Long organizationId);

    @Query("SELECT COUNT(d) FROM Delivery d WHERE d.status = :status AND (:orgId IS NULL OR d.organizationId = :orgId)")
    long countByStatusAndOrganizationId(@Param("status") DeliveryStatus status, @Param("orgId") Long orgId);

    Optional<Delivery> findByDeliveryNumber(String deliveryNumber);

    List<Delivery> findBySalesOrderId(Long salesOrderId);

    List<Delivery> findByDeliveryPersonIdOrderByAssignedAtDesc(Long deliveryPersonId);

    List<Delivery> findByDeliveryPersonIdAndStatusOrderByAssignedAtDesc(Long deliveryPersonId, DeliveryStatus status);

    List<Delivery> findByStatusOrderByAssignedAtDesc(DeliveryStatus status);

    List<Delivery> findAllByOrderByAssignedAtDesc();

    @Query("SELECT COUNT(d) FROM Delivery d WHERE d.status = :status")
    long countByStatus(@Param("status") DeliveryStatus status);
}
