package com.riceerp.backend.security;

import com.riceerp.backend.enums.OrgRole;
import com.riceerp.backend.repository.RolePermissionRepository;
import com.riceerp.backend.service.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RbacSecurityTest {

    private RolePermissionRepository rolePermissionRepository;
    private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        rolePermissionRepository = mock(RolePermissionRepository.class);
        permissionService = new PermissionService(rolePermissionRepository);
    }

    @Test
    void defaultMatrix_adminHasAllPermissions() {
        Set<String> adminPerms = DefaultPermissionMatrix.getDefaults(OrgRole.ADMIN);
        assertTrue(adminPerms.contains("stock:adjust"));
        assertTrue(adminPerms.contains("member:manage"));
        assertTrue(adminPerms.contains("purchase:approve"));
    }

    @Test
    void defaultMatrix_salesCannotAdjustStock() {
        Set<String> salesPerms = DefaultPermissionMatrix.getDefaults(OrgRole.SALES);
        assertFalse(salesPerms.contains("stock:adjust"));
        assertTrue(salesPerms.contains("sale:create"));
        assertFalse(salesPerms.contains("member:manage"));
    }

    @Test
    void defaultMatrix_warehouseCannotApprovePurchase() {
        Set<String> warehousePerms = DefaultPermissionMatrix.getDefaults(OrgRole.WAREHOUSE);
        assertTrue(warehousePerms.contains("stock:adjust"));
        assertFalse(warehousePerms.contains("purchase:approve"));
        assertFalse(warehousePerms.contains("sale:create"));
    }

    @Test
    void permissionService_fallsBackToDefaultsWhenNoDbRows() {
        when(rolePermissionRepository.findByOrganizationIdAndOrgRole(1L, OrgRole.SALES))
                .thenReturn(Collections.emptyList());

        Set<String> effective = permissionService.getEffectivePermissions(1L, OrgRole.SALES);
        assertTrue(effective.contains("sale:create"));
        assertFalse(effective.contains("stock:adjust"));
    }

    @Test
    void tenantIdentifierResolver_returns0WhenContextNull_failsClosed() {
        TenantContext.clear();
        TenantIdentifierResolver resolver = new TenantIdentifierResolver();
        assertEquals(0L, resolver.resolveCurrentTenantIdentifier());
    }

    @Test
    void tenantIdentifierResolver_returnsActiveTenant() {
        TenantContext.setCurrentTenant(42L);
        try {
            TenantIdentifierResolver resolver = new TenantIdentifierResolver();
            assertEquals(42L, resolver.resolveCurrentTenantIdentifier());
        } finally {
            TenantContext.clear();
        }
    }
}
