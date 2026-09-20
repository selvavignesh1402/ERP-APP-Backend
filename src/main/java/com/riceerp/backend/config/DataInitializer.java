package com.riceerp.backend.config;

import com.riceerp.backend.entity.Organization;
import com.riceerp.backend.repository.OrganizationRepository;
import com.riceerp.backend.service.PermissionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final OrganizationRepository organizationRepository;
    private final PermissionService permissionService;

    public DataInitializer(OrganizationRepository organizationRepository,
                           PermissionService permissionService) {
        this.organizationRepository = organizationRepository;
        this.permissionService = permissionService;
    }

    @Override
    public void run(String... args) throws Exception {
        // Seed permission matrices for all existing organizations idempotently
        List<Organization> allOrgs = organizationRepository.findAll();
        for (Organization org : allOrgs) {
            permissionService.seedPermissionsForOrg(org.getId());
        }
    }
}
