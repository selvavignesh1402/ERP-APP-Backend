package com.riceerp.backend.controller;

import com.riceerp.backend.dto.CustomerRequest;
import com.riceerp.backend.entity.Customer;
import com.riceerp.backend.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public Customer createCustomer(@Valid @RequestBody CustomerRequest request) {
        return customerService.createCustomer(request);
    }

    @GetMapping
    public List<Customer> listCustomers(@RequestParam(required = false) String search) {
        return customerService.listCustomers(search);
    }

    @GetMapping("/{id}")
    public Customer getCustomer(@PathVariable Long id) {
        return customerService.getCustomerById(id);
    }

    @PutMapping("/{id}")
    public Customer updateCustomer(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return customerService.updateCustomer(id, request);
    }

    @PutMapping("/{id}/status")
    public Customer toggleStatus(@PathVariable Long id) {
        return customerService.toggleStatus(id);
    }

    @GetMapping("/{id}/balance")
    public Map<String, Object> getBalance(@PathVariable Long id) {
        Customer customer = customerService.getCustomerById(id);
        return Map.of(
                "creditLimit", customer.getCreditLimit(),
                "creditBalance", customer.getCreditBalance(),
                "available", customer.getCreditLimit() - customer.getCreditBalance());
    }
}
