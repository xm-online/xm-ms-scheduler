package com.icthh.xm.ms.scheduler.web.rest;

import com.icthh.xm.commons.gen.api.TenantsApiDelegate;
import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.commons.tenantendpoint.TenantManager;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Component
public class TenantResource implements TenantsApiDelegate {

    private final TenantManager tenantManager;

    @Override
    @Transactional
    @PreAuthorize("hasPermission({'tenant':#tenant}, 'SCHEDULER.TENANT.CREATE')")
    public ResponseEntity<Void> addTenant(Tenant tenant) {
        tenantManager.createTenant(tenant);
        return ResponseEntity.ok().build();
    }

    @Override
    @PreAuthorize("hasPermission({'tenantKey':#tenantKey}, 'SCHEDULER.TENANT.DELETE')")
    public ResponseEntity<Void> deleteTenant(String tenantKey) {
        tenantManager.deleteTenant(tenantKey);
        return ResponseEntity.ok().build();
    }

    @Override
    @PostAuthorize("hasPermission(null, 'SCHEDULER.TENANT.GET_LIST')")
    public ResponseEntity<List<Tenant>> getAllTenantInfo() {
        throw new UnsupportedOperationException();
    }

    @Override
    @PostAuthorize("hasPermission({'returnObject': returnObject.body}, 'SCHEDULER.TENANT.GET_LIST.ITEM')")
    public ResponseEntity<Tenant> getTenant(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    @PreAuthorize("hasPermission({'tenant':#tenant, 'status':#status}, 'SCHEDULER.TENANT.UPDATE')")
    public ResponseEntity<Void> manageTenant(String tenant, String status) {
        tenantManager.manageTenant(tenant, status);
        return ResponseEntity.ok().build();
    }
}
