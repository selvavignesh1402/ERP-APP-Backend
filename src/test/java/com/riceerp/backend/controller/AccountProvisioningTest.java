package com.riceerp.backend.controller;

import com.riceerp.backend.entity.*;
import com.riceerp.backend.repository.*;
import com.riceerp.backend.security.TenantContext;
import com.riceerp.backend.service.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountProvisioningTest {
    private final OrganizationRepository organizations = mock(OrganizationRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final OrganizationMembershipRepository memberships = mock(OrganizationMembershipRepository.class);
    private final PermissionService permissions = mock(PermissionService.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final MasterAdminController master = new MasterAdminController(organizations, users, memberships, encoder, permissions);
    private final OrganizationController team = new OrganizationController(memberships, organizations,
            mock(InviteService.class), users, permissions);
    private final String explicitPassword = "Explicit audit password!";

    @AfterEach
    void clearTenant() { TenantContext.clear(); }

    private Map<String,String> shopRequest(String password) {
        Map<String,String> body = new HashMap<>();
        body.put("name", "Audit shop"); body.put("adminPhone", "9000000000");
        body.put("adminPassword", password);
        return body;
    }

    private Map<String,String> staffRequest(String password) {
        Map<String,String> body = new HashMap<>();
        body.put("name", "Audit staff"); body.put("phoneNumber", "9000000000");
        body.put("role", "SALES"); body.put("password", password);
        return body;
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "short"})
    void newShopAccountRejectsMissingOrShortPasswordBeforeAnyWrite(String password) {
        assertThrows(IllegalArgumentException.class, () -> master.createOrganization(shopRequest(password)));
        verifyNoInteractions(organizations, memberships, encoder, permissions);
        verify(users, never()).save(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "short"})
    void newStaffRejectsMissingOrShortPasswordBeforeAnyWrite(String password) {
        TenantContext.setCurrentTenant(5L);
        assertThrows(IllegalArgumentException.class, () -> team.createStaffDirectly(staffRequest(password)));
        verifyNoInteractions(organizations, memberships);
        verify(users, never()).save(any());
    }

    private void stubSaves() {
        Organization org = mock(Organization.class);
        when(org.getId()).thenReturn(5L);
        when(organizations.save(any())).thenReturn(org);
        when(organizations.findById(5L)).thenReturn(Optional.of(org));
        when(users.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberships.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shopCreationHashesSuppliedPassword() {
        stubSaves();
        when(encoder.encode(explicitPassword)).thenReturn("encoded-audit-value");
        assertEquals(200, master.createOrganization(shopRequest(explicitPassword)).getStatusCode().value());
        verify(users).save(argThat(user -> "encoded-audit-value".equals(user.getPasswordHash())));
    }

    @Test
    void staffCreationPreservesExactPasswordWhenHashing() {
        stubSaves(); TenantContext.setCurrentTenant(5L);
        String withSpaces = " " + explicitPassword + " ";
        team.createStaffDirectly(staffRequest(withSpaces));
        verify(users).save(argThat(user -> new BCryptPasswordEncoder().matches(withSpaces, user.getPasswordHash())));
    }

    @Test
    void existingShopAdminCanBeLinkedWithoutResettingPassword() {
        stubSaves();
        User existing = new User(); existing.setPasswordHash("existing-hash");
        when(users.findByPhoneNumber("9000000000")).thenReturn(Optional.of(existing));
        assertEquals(200, master.createOrganization(shopRequest(null)).getStatusCode().value());
        verify(users, never()).save(any()); verifyNoInteractions(encoder);
        assertEquals("existing-hash", existing.getPasswordHash());
    }

    @Test
    void existingStaffCanBeLinkedWithoutResettingPassword() {
        stubSaves(); TenantContext.setCurrentTenant(5L);
        User existing = new User(); existing.setPasswordHash("existing-hash");
        when(users.findByPhoneNumber("9000000000")).thenReturn(Optional.of(existing));
        assertEquals(200, team.createStaffDirectly(staffRequest(null)).getStatusCode().value());
        verify(users, never()).save(any());
        assertEquals("existing-hash", existing.getPasswordHash());
    }

    @Test
    void bcryptByteLimitRejectsOversizeUnicodeBeforeWriting() {
        String oversize = "界".repeat(25);
        assertThrows(IllegalArgumentException.class, () -> master.createOrganization(shopRequest(oversize)));
        TenantContext.setCurrentTenant(5L);
        assertThrows(IllegalArgumentException.class, () -> team.createStaffDirectly(staffRequest(oversize)));
        verify(users, never()).save(any()); verifyNoInteractions(organizations);
    }
}
