package com.riceerp.backend.repository;

import com.riceerp.backend.entity.Delivery;
import com.riceerp.backend.enums.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    Optional<Delivery> findByDeliveryNumber(String deliveryNumber);

    List<Delivery> findBySalesOrderId(Long salesOrderId);

    List<Delivery> findByDeliveryPersonIdOrderByAssignedAtDesc(Long deliveryPersonId);

    List<Delivery> findByDeliveryPersonIdAndStatusOrderByAssignedAtDesc(Long deliveryPersonId, DeliveryStatus status);

    List<Delivery> findByStatusOrderByAssignedAtDesc(DeliveryStatus status);

    List<Delivery> findAllByOrderByAssignedAtDesc();

    @Query("SELECT COUNT(d) FROM Delivery d WHERE d.status = :status")
    long countByStatus(DeliveryStatus status);
}
