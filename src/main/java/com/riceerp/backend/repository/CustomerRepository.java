package com.riceerp.backend.repository;

import com.riceerp.backend.entity.Customer;
import com.riceerp.backend.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    List<Customer> findByStatus(Status status);

    List<Customer> findByCustomerNameContainingIgnoreCase(String name);

    List<Customer> findByPhoneContaining(String phone);

    @Query("SELECT COALESCE(SUM(c.creditBalance), 0) FROM Customer c WHERE c.status = 'ACTIVE'")
    double sumActiveCreditBalance();
}
