package com.riceerp.backend.repository;

import com.riceerp.backend.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT o FROM Organization o WHERE o.id = :id")
    java.util.Optional<Organization> lockForReconciliation(@org.springframework.data.repository.query.Param("id") Long id);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT o FROM Organization o WHERE o.id = :id")
    java.util.Optional<Organization> lockForDelivery(@org.springframework.data.repository.query.Param("id") Long id);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT o FROM Organization o WHERE o.id = :id")
    java.util.Optional<Organization> lockForPayment(@org.springframework.data.repository.query.Param("id") Long id);
}
