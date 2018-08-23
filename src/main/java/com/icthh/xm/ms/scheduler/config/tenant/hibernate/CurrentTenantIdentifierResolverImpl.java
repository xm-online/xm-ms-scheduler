package com.icthh.xm.ms.scheduler.config.tenant.hibernate;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

@Component
public class CurrentTenantIdentifierResolverImpl implements CurrentTenantIdentifierResolver {

    private final TenantContextHolder tenantContextHolder;

    public CurrentTenantIdentifierResolverImpl(TenantContextHolder tenantContextHolder) {
        this.tenantContextHolder = tenantContextHolder;
    }

    @Override
    public String resolveCurrentTenantIdentifier() {
        return TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder);
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
