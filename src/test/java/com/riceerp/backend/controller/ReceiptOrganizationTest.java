package com.riceerp.backend.controller;

import com.riceerp.backend.entity.Organization;
import com.riceerp.backend.exception.NotFoundException;
import com.riceerp.backend.repository.*;
import com.riceerp.backend.security.TenantContext;
import com.riceerp.backend.service.*;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReceiptOrganizationTest {
    final OrganizationRepository organizations = mock(OrganizationRepository.class);
    final OrganizationController controller = new OrganizationController(mock(OrganizationMembershipRepository.class), organizations,
            mock(InviteService.class), mock(UserRepository.class), mock(PermissionService.class));
    @AfterEach void cleanup() { TenantContext.clear(); }
    @Test void selectedShopIdentityCannotBeChangedByQueryParameter() throws Exception {
        TenantContext.setCurrentTenant(2L);
        Organization shop = new Organization(); shop.setName("Selected Shop");
        when(organizations.findById(2L)).thenReturn(Optional.of(shop));
        MockMvcBuilders.standaloneSetup(controller).build().perform(get("/api/organizations/current?organizationId=99"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.organizationId").value(2))
                .andExpect(jsonPath("$.name").value("Selected Shop"));
        verify(organizations, never()).findById(99L);
    }
    @Test void missingSelectionIsRejected() {
        TenantContext.clear(); assertThrows(AccessDeniedException.class, controller::getCurrentOrganization);
        verifyNoInteractions(organizations);
    }
    @Test void sentinelSelectionIsRejected() {
        TenantContext.setCurrentTenant(0L); assertThrows(AccessDeniedException.class, controller::getCurrentOrganization);
        verifyNoInteractions(organizations);
    }
    @Test void deletedShopDoesNotProduceInventedIdentity() {
        TenantContext.setCurrentTenant(2L); assertThrows(NotFoundException.class, controller::getCurrentOrganization);
    }
}
