package com.riceerp.backend.service;

import com.riceerp.backend.dto.SupplierRequest;
import com.riceerp.backend.entity.Supplier;
import com.riceerp.backend.enums.Status;
import com.riceerp.backend.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @Transactional
    public Supplier createSupplier(SupplierRequest request) {
        Supplier supplier = new Supplier();
        supplier.setSupplierName(request.getSupplierName());
        supplier.setPhone(request.getPhone());
        supplier.setEmail(request.getEmail());
        supplier.setAddress(request.getAddress());
        supplier.setGstNumber(request.getGstNumber());
        supplier.setRating(request.getRating() > 0 ? request.getRating() : 5.0);
        supplier.setCategory(request.getCategory());
        supplier.setStatus(Status.ACTIVE);

        return supplierRepository.save(supplier);
    }

    @Transactional
    public Supplier updateSupplier(Long id, SupplierRequest request) {
        Supplier supplier = getSupplierById(id);

        supplier.setSupplierName(request.getSupplierName());
        supplier.setPhone(request.getPhone());
        supplier.setEmail(request.getEmail());
        supplier.setAddress(request.getAddress());
        supplier.setGstNumber(request.getGstNumber());
        if (request.getRating() > 0) {
            supplier.setRating(request.getRating());
        }
        supplier.setCategory(request.getCategory());

        return supplierRepository.save(supplier);
    }

    public List<Supplier> listSuppliers(String search) {
        if (search != null && !search.trim().isEmpty()) {
            return supplierRepository.findBySupplierNameContainingIgnoreCase(search);
        }
        return supplierRepository.findAll();
    }

    public Supplier getSupplierById(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Supplier not found with id: " + id));
    }

    @Transactional
    public Supplier toggleSupplierStatus(Long id, String status) {
        Supplier supplier = getSupplierById(id);
        supplier.setStatus(Status.valueOf(status.toUpperCase()));
        return supplierRepository.save(supplier);
    }
}
