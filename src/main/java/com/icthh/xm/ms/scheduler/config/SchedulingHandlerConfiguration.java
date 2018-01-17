package com.icthh.xm.ms.scheduler.config;

import com.icthh.xm.ms.scheduler.handler.ScheduledTaskHandler;
import com.icthh.xm.ms.scheduler.handler.ScheduledTaskHandlerMock;
import com.icthh.xm.ms.scheduler.nameresolver.ChannelNameResolver;
import com.icthh.xm.ms.scheduler.nameresolver.TenantAwareChannelNameResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 */
@Configuration
public class SchedulingHandlerConfiguration {

    @Bean
    public ChannelNameResolver channelNameResolver() {
        return new TenantAwareChannelNameResolver();
    }

    @Bean
    @ConditionalOnMissingBean(ScheduledTaskHandler.class)
    public ScheduledTaskHandler scheduledTaskHandlerMock(ChannelNameResolver nameResolver) {
        return new ScheduledTaskHandlerMock(nameResolver);
    }

}
