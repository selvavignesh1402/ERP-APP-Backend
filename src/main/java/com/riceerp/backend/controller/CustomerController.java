package com.riceerp.backend.controller;

import com.riceerp.backend.dto.CustomerRequest;
import com.riceerp.backend.dto.CustomerResponse;
import com.riceerp.backend.entity.Customer;
import com.riceerp.backend.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/customers", "/customers"})
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('customer:create')")
    public CustomerResponse createCustomer(@Valid @RequestBody CustomerRequest request) {
        return CustomerResponse.from(customerService.createCustomer(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('customer:view')")
    public List<CustomerResponse> listCustomers(@RequestParam(required = false) String search) {
        return customerService.listCustomers(search).stream().map(CustomerResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('customer:view')")
    public CustomerResponse getCustomer(@PathVariable Long id) {
        return CustomerResponse.from(customerService.getCustomerById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('customer:edit')")
    public CustomerResponse updateCustomer(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return CustomerResponse.from(customerService.updateCustomer(id, request));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('customer:status')")
    public CustomerResponse toggleStatus(@PathVariable Long id) {
        return CustomerResponse.from(customerService.toggleStatus(id));
    }

    @GetMapping("/{id}/balance")
    @PreAuthorize("hasAuthority('customer:view')")
    public Map<String, Object> getBalance(@PathVariable Long id) {
        Customer customer = customerService.getCustomerById(id);
        return Map.of(
                "creditLimit", customer.getCreditLimit(),
                "creditBalance", customer.getCreditBalance(),
                "available", customer.getCreditLimit()
                        .subtract(customer.getCreditBalance()));
    }
}
