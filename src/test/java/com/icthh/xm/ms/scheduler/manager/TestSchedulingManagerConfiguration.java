package com.icthh.xm.ms.scheduler.manager;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 *
 */
//@Configuration
@TestConfiguration
//    @ComponentScan(basePackages = {"com.icthh.xm.ms.scheduler.repository"})
//    @EnableAutoConfiguration
@EnableBinding
public class TestSchedulingManagerConfiguration {

//        @Autowired
//        TaskMapper taskMapper = Mappers.getMapper(TaskMapper.class);

//        @Bean(initMethod = "init")
//        public SchedulingManager schedulingManager(ThreadPoolTaskScheduler threadPoolTaskScheduler,
//                                                   @Qualifier("taskSrv") TaskServiceExtImpl taskServiceExt) {
//            return new SchedulingManager(threadPoolTaskScheduler, taskServiceExt);
//        }

    @Bean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler
            = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(5);
        threadPoolTaskScheduler.setThreadNamePrefix("xm-sc-thread");
        return threadPoolTaskScheduler;
    }

    @Bean
    public ScheduledTaskHandler scheduledTaskHandler(BinderAwareChannelResolver channelResolver,
                                                     ChannelNameResolver nameResolver) {
        return new ScheduledTaskHandlerImpl(channelResolver, nameResolver);
    }

    @Bean
    public ChannelNameResolver channelNameResolver() {
        return new TenantAwareChannelNameResolver();
    }

//        @Bean
//        public TaskMapper taskMapper() {
//            return Mappers.getMapper(TaskMapper.class);
//        }
//
//        @Bean(name = "taskSrv")
//        public TaskServiceExtImpl taskServiceExt(TaskMapper taskMapper) {
//            return new TaskServiceExtImpl(null, taskMapper);
//        }

}
