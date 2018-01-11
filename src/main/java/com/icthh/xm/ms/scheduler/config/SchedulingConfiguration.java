package com.icthh.xm.ms.scheduler.config;

import com.icthh.xm.ms.scheduler.repository.TaskRepository;
import com.icthh.xm.ms.scheduler.service.SchedulingManager;
import com.icthh.xm.ms.scheduler.service.impl.TaskServiceExt;
import com.icthh.xm.ms.scheduler.service.mapper.TaskMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 *
 */
@Configuration
public class SchedulingConfiguration {

    @Bean(initMethod = "init")
    public SchedulingManager schedulingManager(ThreadPoolTaskScheduler threadPoolTaskScheduler,
                                               TaskServiceExt taskServiceExt) {
        return new SchedulingManager(threadPoolTaskScheduler, taskServiceExt, t -> {
        }, t -> {
        });
    }

    @Bean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler
            = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(5);
        threadPoolTaskScheduler.setThreadNamePrefix("xm-sc-thread");
        return threadPoolTaskScheduler;
    }

//    @Bean
//    public TaskServiceExt taskServiceExt(TaskRepository taskRepository, TaskMapper taskMapper) {
//        return new TaskServiceExt(taskRepository, taskMapper);
//    }

}
