package com.riceerp.backend.config;

import com.riceerp.backend.entity.Organization;
import com.riceerp.backend.entity.User;
import com.riceerp.backend.enums.PlatformRole;
import com.riceerp.backend.repository.OrganizationRepository;
import com.riceerp.backend.repository.UserRepository;
import com.riceerp.backend.service.PermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MasterAdminBootstrapTest {
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final String explicitPassword = "Explicit audit password!";

    private MasterAdminBootstrap bootstrap(String password) {
        return new MasterAdminBootstrap(users, encoder, "9000000000", "Platform owner", password);
    }

    @Test
    void bootstrapBeanIsAbsentUnlessExplicitlyEnabled() {
        new ApplicationContextRunner().withUserConfiguration(MasterAdminBootstrap.class)
                .run(context -> assertTrue(context.getBeansOfType(MasterAdminBootstrap.class).isEmpty()));
    }

    @Test
    void explicitEnablementRegistersBootstrap() {
        new ApplicationContextRunner().withUserConfiguration(MasterAdminBootstrap.class)
                .withBean(UserRepository.class, () -> users)
                .withBean(PasswordEncoder.class, () -> encoder)
                .withPropertyValues("app.bootstrap.master-admin.enabled=true")
                .run(context -> assertEquals(1, context.getBeansOfType(MasterAdminBootstrap.class).size()));
    }

    @Test
    void enabledBootstrapWithoutPasswordFailsBeforeWriting() {
        assertThrows(IllegalArgumentException.class, () -> bootstrap("").run());
        verify(users, never()).save(any());
        verifyNoInteractions(encoder);
    }

    @Test
    void enabledBootstrapWithoutIdentityFailsBeforeWriting() {
        assertThrows(IllegalArgumentException.class,
                () -> new MasterAdminBootstrap(users, encoder, "", "", explicitPassword).run());
        verify(users, never()).save(any());
    }

    @Test
    void bootstrapCreatesFirstAdminWithEncodedExplicitPasswordOnly() {
        when(encoder.encode(explicitPassword)).thenReturn("encoded-audit-value");
        bootstrap(explicitPassword).run();
        verify(users).save(argThat(user ->
                user.getPlatformRole() == PlatformRole.MASTER_ADMIN && user.isActive()
                && "9000000000".equals(user.getPhoneNumber())
                && "encoded-audit-value".equals(user.getPasswordHash())));
    }

    @Test
    void existingAdminPreventsRecreationOrPasswordResetEvenWithoutBootstrapCredentials() {
        when(users.countByPlatformRole(PlatformRole.MASTER_ADMIN)).thenReturn(1L);
        bootstrap("").run();
        verify(users, never()).save(any());
        verifyNoInteractions(encoder);
    }

    @Test
    void bootstrapCannotPromoteAnExistingOrdinaryAccount() {
        when(users.existsByPhoneNumber("9000000000")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> bootstrap(explicitPassword).run());
        verify(users, never()).save(any());
        verifyNoInteractions(encoder);
    }

    @Test
    void normalInitializerOnlySeedsExistingOrganizationPermissions() throws Exception {
        OrganizationRepository organizations = mock(OrganizationRepository.class);
        PermissionService permissions = mock(PermissionService.class);
        Organization org = mock(Organization.class);
        when(org.getId()).thenReturn(5L);
        when(organizations.findAll()).thenReturn(List.of(org));
        new DataInitializer(organizations, permissions).run();
        verify(permissions).seedPermissionsForOrg(5L);
        verify(organizations, never()).save(any());
    }
}
