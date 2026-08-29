package com.riceerp.backend.repository;

import com.riceerp.backend.entity.Product;
import com.riceerp.backend.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByProductNameContainingIgnoreCase(String productName);

    List<Product> findByCategoryIgnoreCase(String category);

    List<Product> findByStatus(Status status);

    @Query("SELECT COALESCE(SUM(p.stock), 0) FROM Product p WHERE p.status = 'ACTIVE'")
    double sumStockByActiveStatus();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.stock < p.minimumStock AND p.status = 'ACTIVE'")
    long countLowStock();
}
