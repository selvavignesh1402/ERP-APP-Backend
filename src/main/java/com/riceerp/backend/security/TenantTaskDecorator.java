package com.riceerp.backend.security;

import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;
import org.springframework.security.core.context.SecurityContextHolder;

/** Used by Boot's application task executor; scopes a task and restores pooled-thread state. */
@Component
public final class TenantTaskDecorator implements TaskDecorator {
    @Override
    public Runnable decorate(Runnable action) {
        Long tenant = TenantContext.getCurrentTenant();
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return () -> {
            Long previousTenant = TenantContext.getCurrentTenant();
            var previousSecurity = SecurityContextHolder.getContext();
            try {
                if (tenant == null) TenantContext.clear(); else TenantContext.setCurrentTenant(tenant);
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
                action.run();
            } finally {
                if (previousTenant == null) TenantContext.clear(); else TenantContext.setCurrentTenant(previousTenant);
                SecurityContextHolder.setContext(previousSecurity);
            }
        };
    }
}
