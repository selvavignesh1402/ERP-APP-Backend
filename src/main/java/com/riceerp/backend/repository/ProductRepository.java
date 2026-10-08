package com.riceerp.backend.repository;

import com.riceerp.backend.entity.Product;
import com.riceerp.backend.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findForStockUpdate(@org.springframework.data.repository.query.Param("id") Long id);

    @Query(value = "select coalesce(sum(i.ordered_quantity - i.delivered_quantity), 0) from sales_order_items i " +
            "join sales_orders o on o.id = i.sales_order_id join products p on p.id = i.product_id " +
            "where p.id = :id and o.organization_id = p.organization_id and o.status in " +
            "('CONFIRMED','PROCESSING','READY_FOR_DELIVERY','OUT_FOR_DELIVERY','PARTIALLY_DELIVERED')", nativeQuery = true)
    double reservedQuantity(@org.springframework.data.repository.query.Param("id") Long id);
    @Query(value = "select i.product_id, sum(i.ordered_quantity-i.delivered_quantity) from sales_order_items i " +
            "join sales_orders o on o.id=i.sales_order_id join products p on p.id=i.product_id " +
            "where i.product_id in (:ids) and o.organization_id=p.organization_id and o.status in " +
            "('CONFIRMED','PROCESSING','READY_FOR_DELIVERY','OUT_FOR_DELIVERY','PARTIALLY_DELIVERED') group by i.product_id", nativeQuery=true)
    List<Object[]> reservedQuantities(@org.springframework.data.repository.query.Param("ids") List<Long> ids);

    Optional<Product> findByIdAndOrganizationId(Long id, Long organizationId);
    List<Product> findByProductNameContainingIgnoreCase(String productName);

    List<Product> findByCategoryIgnoreCase(String category);

    List<Product> findByStatus(Status status);

    @Query("SELECT COALESCE(SUM(p.stock), 0) FROM Product p WHERE p.status = 'ACTIVE'")
    double sumStockByActiveStatus();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.stock < p.minimumStock AND p.status = 'ACTIVE'")
    long countLowStock();
}
