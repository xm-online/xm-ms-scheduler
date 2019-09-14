package com.icthh.xm.ms.scheduler.config.tenant;

import com.icthh.xm.commons.tenantendpoint.TenantManager;
import com.icthh.xm.commons.tenantendpoint.provisioner.TenantAbilityCheckerProvisioner;
import com.icthh.xm.commons.tenantendpoint.provisioner.TenantDatabaseProvisioner;
import com.icthh.xm.commons.tenantendpoint.provisioner.TenantListProvisioner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;

@Slf4j
@org.springframework.context.annotation.Configuration
public class TenantManagerConfiguration {

    @Bean
    public TenantManager tenantManager(TenantAbilityCheckerProvisioner abilityCheckerProvisioner,
                                       TenantDatabaseProvisioner databaseProvisioner,
                                       TenantListProvisioner tenantListProvisioner) {

        TenantManager manager = TenantManager.builder()
                                             .service(abilityCheckerProvisioner)
                                             .service(tenantListProvisioner)
                                             .service(databaseProvisioner)
                                             .build();
        log.info("Configured tenant manager: {}", manager);
        return manager;
    }

}
