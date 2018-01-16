package com.icthh.xm.ms.scheduler.manager;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 *
 */
@TestConfiguration
public class TestSchedulingManagerConfiguration {

    @Bean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler
            = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(5);
        threadPoolTaskScheduler.setThreadNamePrefix("xm-sc-thread");
        return threadPoolTaskScheduler;
    }

    @Bean("testChannelNameResolver")
    @Primary
    public TestChannelNameResolver testChannelNameResolver() {
        return new TestChannelNameResolver();
    }

}
