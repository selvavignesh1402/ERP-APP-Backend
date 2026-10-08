package com.riceerp.backend.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.*;

class TenantTaskDecoratorTest {
    @AfterEach void clean() { TenantContext.clear(); SecurityContextHolder.clearContext(); }

    @Test void capturesSubmittingTenantAndRestoresWorkerEvenOnFailure() {
        TenantContext.setCurrentTenant(12L);
        var principal = new UsernamePasswordAuthenticationToken("user", null);
        SecurityContextHolder.getContext().setAuthentication(principal);
        Runnable task = new TenantTaskDecorator().decorate(() -> {
            assertEquals(12L, TenantContext.getCurrentTenant());
            assertSame(principal, SecurityContextHolder.getContext().getAuthentication());
            throw new IllegalStateException("task failed");
        });
        TenantContext.setCurrentTenant(99L);
        SecurityContextHolder.clearContext();
        var workerSecurity = SecurityContextHolder.getContext();
        assertThrows(IllegalStateException.class, task::run);
        assertEquals(99L, TenantContext.getCurrentTenant());
        assertSame(workerSecurity, SecurityContextHolder.getContext());
    }

    @Test void anonymousSubmissionCannotInheritWorkerTenant() {
        Runnable task = new TenantTaskDecorator().decorate(() -> {
            assertNull(TenantContext.getCurrentTenant());
            assertEquals(0L, new TenantIdentifierResolver().resolveCurrentTenantIdentifier());
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        });
        TenantContext.setCurrentTenant(99L);
        task.run();
        assertEquals(99L, TenantContext.getCurrentTenant());
    }
}
