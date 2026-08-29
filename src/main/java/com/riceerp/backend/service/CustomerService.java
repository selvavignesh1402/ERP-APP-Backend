package com.riceerp.backend.service;

import com.riceerp.backend.dto.CustomerRequest;
import com.riceerp.backend.entity.Customer;
import com.riceerp.backend.enums.Status;
import com.riceerp.backend.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Customer createCustomer(CustomerRequest request) {
        Customer customer = new Customer();
        customer.setCustomerName(request.getCustomerName());
        customer.setPhone(request.getPhone());
        customer.setEmail(request.getEmail());
        customer.setAddress(request.getAddress());
        customer.setGstNumber(request.getGstNumber());
        customer.setCreditLimit(request.getCreditLimit());
        customer.setStatus(Status.ACTIVE);
        return customerRepository.save(customer);
    }

    public Customer updateCustomer(Long id, CustomerRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));

        customer.setCustomerName(request.getCustomerName());
        customer.setPhone(request.getPhone());
        customer.setEmail(request.getEmail());
        customer.setAddress(request.getAddress());
        customer.setGstNumber(request.getGstNumber());
        customer.setCreditLimit(request.getCreditLimit());
        return customerRepository.save(customer);
    }

    public Customer toggleStatus(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));

        customer.setStatus(customer.getStatus() == Status.ACTIVE ? Status.INACTIVE : Status.ACTIVE);
        return customerRepository.save(customer);
    }

    public List<Customer> listCustomers(String search) {
        if (search != null && !search.trim().isEmpty()) {
            return customerRepository.findByCustomerNameContainingIgnoreCase(search);
        }
        return customerRepository.findAll();
    }

    public List<Customer> listActiveCustomers() {
        return customerRepository.findByStatus(Status.ACTIVE);
    }

    public Customer getCustomerById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
    }
}
