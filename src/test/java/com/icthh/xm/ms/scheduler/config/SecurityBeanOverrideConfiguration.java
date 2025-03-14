package com.icthh.xm.ms.scheduler.config;

import com.icthh.xm.ms.scheduler.listener.SchedulerTaskDynamicConsumerConfiguration;
import org.springframework.cloud.client.loadbalancer.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import static org.mockito.Mockito.mock;

/**
 * Overrides UAA specific beans, so they do not interfere the testing
 * This configuration must be included in @SpringBootTest in order to take effect.
 */
@Configuration
public class SecurityBeanOverrideConfiguration {

    @Bean
    @Primary
    public RestTemplate loadBalancedRestTemplate(RestTemplateCustomizer customizer) {
        return null;
    }

    @Bean
    @Primary
    public SchedulerTaskDynamicConsumerConfiguration schedulerTaskDynamicConsumerConfiguration() {
        return mock(SchedulerTaskDynamicConsumerConfiguration.class);
    }
}
