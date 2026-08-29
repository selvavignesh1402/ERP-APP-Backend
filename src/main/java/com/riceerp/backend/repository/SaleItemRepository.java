package com.riceerp.backend.repository;

import com.riceerp.backend.entity.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {
    List<SaleItem> findBySaleId(Long saleId);

    @Query("SELECT COALESCE(SUM(si.quantity), 0) FROM SaleItem si WHERE si.sale.saleDate BETWEEN :start AND :end")
    double sumQuantityBySaleDateBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(si.quantity), 0) FROM SaleItem si WHERE si.product.id = :productId AND si.sale.saleDate BETWEEN :start AND :end")
    double sumQuantityByProductIdAndSaleDateBetween(@Param("productId") Long productId,
                                                    @Param("start") LocalDateTime start,
                                                    @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(si.quantity * si.price), 0) FROM SaleItem si WHERE si.product.id = :productId AND si.sale.saleDate BETWEEN :start AND :end")
    double sumRevenueByProductIdAndSaleDateBetween(@Param("productId") Long productId,
                                                   @Param("start") LocalDateTime start,
                                                   @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(DISTINCT si.sale.id) FROM SaleItem si WHERE si.product.id = :productId AND si.sale.saleDate BETWEEN :start AND :end")
    long countSalesByProductIdAndSaleDateBetween(@Param("productId") Long productId,
                                                 @Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end);

    @Query("SELECT si FROM SaleItem si WHERE si.product.id = :productId AND si.sale.saleDate BETWEEN :start AND :end ORDER BY si.sale.saleDate DESC")
    List<SaleItem> findByProductIdAndSaleDateBetweenOrderBySaleDateDesc(@Param("productId") Long productId,
                                                                       @Param("start") LocalDateTime start,
                                                                       @Param("end") LocalDateTime end);
}
