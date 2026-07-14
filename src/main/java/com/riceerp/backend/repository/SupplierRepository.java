package com.riceerp.backend.repository;

import com.riceerp.backend.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    List<Supplier> findBySupplierNameContainingIgnoreCase(String name);
    List<Supplier> findByStatus(String status);
}
