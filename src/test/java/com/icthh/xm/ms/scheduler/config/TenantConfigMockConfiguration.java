package com.icthh.xm.ms.scheduler.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.config.client.service.TenantAliasService;
import com.icthh.xm.commons.web.spring.TenantVerifyInterceptor;
import com.icthh.xm.ms.scheduler.TaskTestUtil;

import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TenantConfigMockConfiguration {

    @Bean
    @Primary
    public TenantListRepository tenantListRepository() {
        TenantListRepository mockTenantListRepository = mock(TenantListRepository.class);
        Set<String> set = new HashSet<>();
        set.add(TaskTestUtil.TEST_TENANT);
        set.add(TaskTestUtil.XM_TENANT);
        when(mockTenantListRepository.getTenants()).thenReturn(set);
        return mockTenantListRepository;
    }

    @Bean
    @Primary
    public TenantConfigRepository tenantConfigRepository() {
        return mock(TenantConfigRepository.class);
    }

    @Bean
    @Primary
    public TenantVerifyInterceptor tenantVerifyInterceptor() {
        return mock(TenantVerifyInterceptor.class);
    }

    @Bean
    public TenantAliasService tenantAliasService() {
        return new TenantAliasService(mock(CommonConfigRepository.class), mock(TenantListRepository.class));
    }
}
