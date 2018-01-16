package com.icthh.xm.ms.scheduler.config;

import com.icthh.xm.ms.scheduler.handler.ScheduledTaskHandler;
import com.icthh.xm.ms.scheduler.handler.ScheduledTaskHandlerImpl;
import com.icthh.xm.ms.scheduler.nameresolver.ChannelNameResolver;
import com.icthh.xm.ms.scheduler.nameresolver.TenantAwareChannelNameResolver;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.context.annotation.Bean;

/**
 * Configures Spring Cloud Stream support.
 *
 * See http://docs.spring.io/spring-cloud-stream/docs/current/reference/htmlsingle/
 * for more information.
 */
@EnableBinding
public class MessagingConfiguration {

    @Bean
    public ChannelNameResolver channelNameResolver() {
        return new TenantAwareChannelNameResolver();
    }

    @Bean
    public ScheduledTaskHandler scheduledTaskHandler(BinderAwareChannelResolver channelResolver,
                                                     ChannelNameResolver nameResolver) {
        return new ScheduledTaskHandlerImpl(channelResolver, nameResolver);
    }

}
