package com.icthh.xm.ms.scheduler.config.tenant;

import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.commons.migration.db.tenant.provisioner.TenantDatabaseProvisioner;
import com.icthh.xm.commons.tenantendpoint.TenantManager;
import com.icthh.xm.commons.tenantendpoint.provisioner.TenantAbilityCheckerProvisioner;
import com.icthh.xm.commons.tenantendpoint.provisioner.TenantConfigProvisioner;
import com.icthh.xm.commons.tenantendpoint.provisioner.TenantListProvisioner;
import com.icthh.xm.ms.scheduler.AbstractUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class TenantManagerConfigurationUnitTest extends AbstractUnitTest {

    private TenantManager tenantManager;

    @Spy
    private TenantManagerConfiguration configuration = new TenantManagerConfiguration();

    @Mock
    private TenantAbilityCheckerProvisioner abilityCheckerProvisioner;
    @Mock
    private TenantDatabaseProvisioner databaseProvisioner;
    @Mock
    private TenantConfigProvisioner configProvisioner;
    @Mock
    private TenantListProvisioner tenantListProvisioner;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        tenantManager = configuration.tenantManager(abilityCheckerProvisioner, databaseProvisioner,
            configProvisioner, tenantListProvisioner);
    }

    @Test
    public void testCreateTenantProvisioningOrder() {

        tenantManager.createTenant(new Tenant().tenantKey("newtenant"));

        InOrder inOrder = Mockito.inOrder(abilityCheckerProvisioner,
            tenantListProvisioner,
            databaseProvisioner);

        inOrder.verify(abilityCheckerProvisioner).createTenant(any(Tenant.class));
        inOrder.verify(tenantListProvisioner).createTenant(any(Tenant.class));
        inOrder.verify(databaseProvisioner).createTenant(any(Tenant.class));

        verifyNoMoreInteractions(abilityCheckerProvisioner,
            tenantListProvisioner,
            databaseProvisioner);
    }

}
