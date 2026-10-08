package com.riceerp.backend.service;

import com.riceerp.backend.repository.OrganizationRepository;
import com.riceerp.backend.security.TenantContext;
import com.riceerp.backend.exception.NotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class ProcurementLock {
    private final OrganizationRepository organizations;
    public ProcurementLock(OrganizationRepository organizations) { this.organizations = organizations; }
    public Long tenant() {
        Long id = TenantContext.getCurrentTenant();
        if (id == null || id <= 0) throw new AccessDeniedException("Select an organization first");
        return id;
    }
    // Call before loading procurement entities in a READ_COMMITTED transaction.
    public Long acquire() {
        Long id = tenant();
        organizations.lockForReconciliation(id).orElseThrow(() -> new NotFoundException("Organization not found"));
        return id;
    }
}
