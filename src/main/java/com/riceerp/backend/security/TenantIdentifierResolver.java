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
        // Fallback for background tasks or app startup
        return 1L; 
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
