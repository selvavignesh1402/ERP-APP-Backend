package com.riceerp.backend.repository;

import com.riceerp.backend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByProductNameContainingIgnoreCase(String productName);
    List<Product> findByCategoryIgnoreCase(String category);
    List<Product> findByStatus(String status);
}
