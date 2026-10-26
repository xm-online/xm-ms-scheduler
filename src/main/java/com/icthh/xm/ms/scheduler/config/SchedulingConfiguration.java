package com.icthh.xm.ms.scheduler.config;

import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.ms.scheduler.handler.ScheduledTaskHandler;
import com.icthh.xm.ms.scheduler.manager.LocalOwnership;
import com.icthh.xm.ms.scheduler.manager.SchedulingManager;
import com.icthh.xm.ms.scheduler.manager.TaskOwnership;
import com.icthh.xm.ms.scheduler.repository.TaskRepository;
import com.icthh.xm.ms.scheduler.service.LocalTaskChangeNotifier;
import com.icthh.xm.ms.scheduler.service.SystemTaskService;
import com.icthh.xm.ms.scheduler.service.TaskChangeNotifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * The scheduling configuration.
 */
@Configuration
public class SchedulingConfiguration {

    @Bean(destroyMethod = "destroy")
    public SchedulingManager schedulingManager(TenantContextHolder tenantContextHolder,
                                               ThreadPoolTaskScheduler threadPoolTaskScheduler,
                                               SystemTaskService systemTaskService,
                                               ScheduledTaskHandler handler,
                                               TenantListRepository tenantListRepository,
                                               TaskRepository taskRepository,
                                               TaskOwnership ownership) {
        return new SchedulingManager(tenantContextHolder, threadPoolTaskScheduler, systemTaskService, handler,
                                     null, null, tenantListRepository, taskRepository, ownership);
    }

    /** Single instance mode (default): everything is owned. Another module replaces it by a bean of the same name. */
    @Bean
    @ConditionalOnMissingBean(name = "taskOwnership")
    public TaskOwnership taskOwnership() {
        return new LocalOwnership();
    }

    /**
     * Single instance mode (default): changes go straight to the scheduling manager. Another module replaces it by a
     * bean of the same name.
     */
    @Bean
    @ConditionalOnMissingBean(name = "taskChangeNotifier")
    public TaskChangeNotifier taskChangeNotifier(@Lazy SchedulingManager schedulingManager) {
        return new LocalTaskChangeNotifier(schedulingManager);
    }

    /**
     * Creates the thread pool  task scheduler.
     * @param applicationProperties the application properties
     * @return the scheduler
     */
    @Bean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler(ApplicationProperties applicationProperties) {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(applicationProperties.getScheduler().getThreadPoolSize());
        threadPoolTaskScheduler.setThreadNamePrefix("xm-sc-thread");
        return threadPoolTaskScheduler;
    }

}
