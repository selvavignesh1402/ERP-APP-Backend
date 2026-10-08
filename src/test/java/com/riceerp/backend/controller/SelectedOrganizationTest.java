package com.riceerp.backend.controller;

import com.riceerp.backend.entity.*;
import com.riceerp.backend.enums.OrgRole;
import com.riceerp.backend.enums.PlatformRole;
import com.riceerp.backend.repository.*;
import com.riceerp.backend.security.TenantContext;
import com.riceerp.backend.service.*;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SelectedOrganizationTest {
    private final UserRepository users = mock(UserRepository.class);
    private final OrganizationMembershipRepository memberships = mock(OrganizationMembershipRepository.class);
    private final OrganizationRepository organizations = mock(OrganizationRepository.class);
    private final PermissionService permissions = mock(PermissionService.class);
    private final AuthController controller = new AuthController(mock(OtpService.class), users,
            memberships, organizations, mock(PasswordEncoder.class), permissions);
    private final User user = new User();

    @BeforeEach void setup() {
        TenantContext.clear();
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", 7L);
        user.setPlatformRole(PlatformRole.USER);
        when(users.findById(7L)).thenReturn(Optional.of(user));
    }
    @AfterEach void cleanup() { TenantContext.clear(); }

    private Map<String, Object> profile() {
        return controller.me(new UsernamePasswordAuthenticationToken(7L, null, List.of()));
    }
    private OrganizationMembership membership(long id, OrgRole role) {
        Organization organization = new Organization();
        org.springframework.test.util.ReflectionTestUtils.setField(organization, "id", id);
        OrganizationMembership member = new OrganizationMembership();
        member.setOrganization(organization); member.setUser(user); member.setRole(role); member.setActive(true);
        return member;
    }

    @Test void selectedShopControlsRoleAndPermissionsInsteadOfFirstMembership() {
        OrganizationMembership first = membership(1L, OrgRole.ADMIN);
        OrganizationMembership selected = membership(2L, OrgRole.SALES);
        when(memberships.findByUserId(7L)).thenReturn(List.of(first, selected));
        when(memberships.findByUserIdAndOrganizationIdAndIsActiveTrue(7L, 2L)).thenReturn(Optional.of(selected));
        when(permissions.getEffectivePermissions(2L, OrgRole.SALES)).thenReturn(Set.of("sale:create"));
        TenantContext.setCurrentTenant(2L);
        Map<String, Object> result = profile();
        assertEquals(2L, result.get("organizationId"));
        assertEquals("SALES", result.get("role"));
        assertEquals("SALES", result.get("orgRole"));
        assertEquals(Set.of("sale:create"), result.get("permissions"));
        verify(memberships, never()).findByUserId(anyLong());
        verifyNoInteractions(organizations);
    }

    @Test void noSelectedShopDoesNotProvisionOrChooseMembership() {
        Map<String, Object> result = profile();
        assertNull(result.get("organizationId")); assertNull(result.get("orgRole"));
        assertEquals("USER", result.get("role")); assertEquals(Set.of(), result.get("permissions"));
        verifyNoInteractions(memberships, organizations, permissions);
    }

    @Test void missingOrInactiveSelectedMembershipIsDeniedWithoutFallback() {
        TenantContext.setCurrentTenant(2L);
        assertThrows(AccessDeniedException.class, this::profile);
        verifyNoInteractions(organizations, permissions);
        verify(memberships, never()).findByUserId(anyLong());
    }

    @Test void masterAdminWithoutSelectedShopKeepsGlobalRole() {
        user.setPlatformRole(PlatformRole.MASTER_ADMIN);
        Map<String, Object> result = profile();
        assertEquals("MASTER_ADMIN", result.get("role"));
        assertNull(result.get("organizationId"));
        assertFalse(((Set<?>) result.get("permissions")).isEmpty());
        verifyNoInteractions(memberships, organizations);
    }

    @Test void masterAdminSelectedShopDoesNotRequireMembership() {
        user.setPlatformRole(PlatformRole.MASTER_ADMIN);
        TenantContext.setCurrentTenant(2L);
        Map<String, Object> result = profile();
        assertEquals(2L, result.get("organizationId"));
        assertEquals("MASTER_ADMIN", result.get("role")); assertNull(result.get("orgRole"));
        verifyNoInteractions(organizations);
    }
}
