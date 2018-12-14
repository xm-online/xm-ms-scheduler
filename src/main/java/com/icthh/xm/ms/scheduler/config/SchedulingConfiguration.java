package com.icthh.xm.ms.scheduler.config;

import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.ms.scheduler.handler.ScheduledTaskHandler;
import com.icthh.xm.ms.scheduler.manager.SchedulingManager;
import com.icthh.xm.ms.scheduler.repository.TaskRepository;
import com.icthh.xm.ms.scheduler.service.SystemTaskService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 *
 */
@Configuration
public class SchedulingConfiguration {

    @Bean(destroyMethod = "destroy")
    public SchedulingManager schedulingManager(TenantContextHolder tenantContextHolder,
                                               ThreadPoolTaskScheduler threadPoolTaskScheduler,
                                               SystemTaskService systemTaskService,
                                               ScheduledTaskHandler handler,
                                               TenantListRepository tenantListRepository,
                                               TaskRepository taskRepository) {
        return new SchedulingManager(tenantContextHolder, threadPoolTaskScheduler,
                                     systemTaskService, handler, tenantListRepository, taskRepository);
    }

    @Bean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler(ApplicationProperties applicationProperties) {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(applicationProperties.getScheduler().getThreadPoolSize());
        threadPoolTaskScheduler.setThreadNamePrefix("xm-sc-thread");
        return threadPoolTaskScheduler;
    }

}
