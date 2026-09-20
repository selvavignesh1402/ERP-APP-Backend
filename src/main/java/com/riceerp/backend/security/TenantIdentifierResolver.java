package com.riceerp.backend.security;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<Long> {

    @Override
    public Long resolveCurrentTenantIdentifier() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            return tenantId;
        }
        // Fail-closed tenant resolution: 0L matches no tenant data instead of leaking org 1
        return 0L; 
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
