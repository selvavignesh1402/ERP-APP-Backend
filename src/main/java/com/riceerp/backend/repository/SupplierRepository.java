package com.riceerp.backend.repository;

import com.riceerp.backend.entity.Supplier;
import com.riceerp.backend.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    List<Supplier> findBySupplierNameContainingIgnoreCase(String name);

    List<Supplier> findByStatus(Status status);

    long countByStatus(Status status);
}
