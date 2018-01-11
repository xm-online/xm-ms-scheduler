package com.icthh.xm.ms.scheduler.config;

import com.icthh.xm.ms.scheduler.manager.SchedulingManager;
import com.icthh.xm.ms.scheduler.service.TaskServiceExt;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 *
 */
@Configuration
public class SchedulingConfiguration {

    @Bean(initMethod = "init", destroyMethod = "destroy")
    public SchedulingManager schedulingManager(ThreadPoolTaskScheduler threadPoolTaskScheduler,
                                               TaskServiceExt taskServiceExt) {
        return new SchedulingManager(threadPoolTaskScheduler, taskServiceExt);
    }

    @Bean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler
            = new ThreadPoolTaskScheduler();

        // TODO - move to parameters
        threadPoolTaskScheduler.setPoolSize(5);
        threadPoolTaskScheduler.setThreadNamePrefix("xm-sc-thread");
        return threadPoolTaskScheduler;
    }

//    @Bean
//    public TaskServiceExt taskServiceExt(TaskRepository taskRepository, TaskMapper taskMapper) {
//        return new TaskServiceExt(taskRepository, taskMapper);
//    }

}
