package com.riceerp.backend.security;

import com.riceerp.backend.entity.*;
import com.riceerp.backend.enums.*;
import com.riceerp.backend.repository.*;
import com.riceerp.backend.service.PermissionService;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CurrentAuthorizationTest {
    @AfterEach void clear() { SecurityContextHolder.clearContext(); TenantContext.clear(); }
    @Test void oldMasterAdminTokenCannotPreserveDemotedPrivileges() throws Exception {
        var users = mock(UserRepository.class); var members = mock(OrganizationMembershipRepository.class);
        var permissions = mock(PermissionService.class);
        var user = new User(); user.setActive(true); user.setPlatformRole(PlatformRole.USER);
        when(users.findById(8L)).thenReturn(Optional.of(user));
        var membership = new OrganizationMembership(); membership.setRole(OrgRole.SALES);
        when(members.findByUserIdAndOrganizationIdAndIsActiveTrue(8L, 2L)).thenReturn(Optional.of(membership));
        when(permissions.getEffectivePermissions(2L, OrgRole.SALES)).thenReturn(Set.of("sale:view"));
        var request = new MockHttpServletRequest("GET", "/sales");
        request.addHeader("Authorization", "Bearer " + JwtUtil.generateToken(8L, "test", PlatformRole.MASTER_ADMIN, 2L, OrgRole.ADMIN));
        var response = new MockHttpServletResponse();
        new JwtFilter(users, members, permissions).doFilter(request, response, (req, res) -> {
            var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().map(Object::toString).toList();
            assertTrue(authorities.contains("sale:view")); assertFalse(authorities.contains("member:manage"));
            assertFalse(authorities.contains("ROLE_MASTER_ADMIN")); assertEquals(2L, TenantContext.getCurrentTenant());
        });
        assertEquals(200, response.getStatus()); assertNull(TenantContext.getCurrentTenant());
    }
    @Test void demotedMasterWithoutMembershipIsRejected() throws Exception {
        var users = mock(UserRepository.class); var members = mock(OrganizationMembershipRepository.class);
        var user = new User(); user.setActive(true); user.setPlatformRole(PlatformRole.USER);
        when(users.findById(8L)).thenReturn(Optional.of(user));
        var request = new MockHttpServletRequest("GET", "/sales");
        request.addHeader("Authorization", "Bearer " + JwtUtil.generateToken(8L, "test", PlatformRole.MASTER_ADMIN, 2L, OrgRole.ADMIN));
        var response = new MockHttpServletResponse();
        new JwtFilter(users, members, mock(PermissionService.class)).doFilter(request, response, (req, res) -> fail("Must not reach controller"));
        assertEquals(401, response.getStatus());
    }
}
