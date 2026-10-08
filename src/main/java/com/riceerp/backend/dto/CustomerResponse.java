package com.riceerp.backend.dto;
import java.math.BigDecimal;

import com.riceerp.backend.entity.Customer;
import com.riceerp.backend.enums.Status;

import java.time.LocalDateTime;

/**
 * Response shape for a customer. Decouples the public API from the persistence
 * model so a column rename in {@link Customer} cannot become a breaking API change.
 *
 * <p>Field names intentionally match the previous entity serialization so
 * existing clients keep working.
 */
public class CustomerResponse {

    private Long id;
    private Long organizationId;
    private String customerName;
    private String phone;
    private String email;
    private String address;
    private String gstNumber;
    private BigDecimal creditLimit = BigDecimal.ZERO;
    private BigDecimal creditBalance = BigDecimal.ZERO;
    private Status status;
    private LocalDateTime createdAt;

    public static CustomerResponse from(Customer c) {
        if (c == null) return null;
        CustomerResponse r = new CustomerResponse();
        r.id = c.getId();
        r.organizationId = c.getOrganizationId();
        r.customerName = c.getCustomerName();
        r.phone = c.getPhone();
        r.email = c.getEmail();
        r.address = c.getAddress();
        r.gstNumber = c.getGstNumber();
        r.creditLimit = c.getCreditLimit();
        r.creditBalance = c.getCreditBalance();
        r.status = c.getStatus();
        r.createdAt = c.getCreatedAt();
        return r;
    }

    public Long getId() { return id; }
    public Long getOrganizationId() { return organizationId; }
    public String getCustomerName() { return customerName; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getAddress() { return address; }
    public String getGstNumber() { return gstNumber; }
    public BigDecimal getCreditLimit() { return creditLimit; }
    public BigDecimal getCreditBalance() { return creditBalance; }
    public Status getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}