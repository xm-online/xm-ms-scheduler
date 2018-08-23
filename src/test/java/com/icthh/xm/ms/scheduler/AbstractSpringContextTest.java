package com.icthh.xm.ms.scheduler;

import com.icthh.xm.ms.scheduler.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.ms.scheduler.config.TenantConfigMockConfiguration;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {SchedulerApp.class,
    SecurityBeanOverrideConfiguration.class,
    TenantConfigMockConfiguration.class})
public abstract class AbstractSpringContextTest {
}
