package com.icthh.xm.ms.scheduler.config;

import com.icthh.xm.ms.scheduler.handler.ScheduledTaskHandler;
import com.icthh.xm.ms.scheduler.handler.ScheduledTaskHandlerMock;
import com.icthh.xm.ms.scheduler.nameresolver.ChannelNameResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The scheduling handler configuration.
 */
@Configuration
public class SchedulingHandlerOverrideConfiguration {

    @Bean
    @ConditionalOnMissingBean(ScheduledTaskHandler.class)
    public ScheduledTaskHandler scheduledTaskHandlerMock(ChannelNameResolver nameResolver) {
        return new ScheduledTaskHandlerMock(nameResolver);
    }

}
