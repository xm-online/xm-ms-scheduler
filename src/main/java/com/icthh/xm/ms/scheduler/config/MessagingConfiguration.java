package com.icthh.xm.ms.scheduler.config;

import com.icthh.xm.ms.scheduler.handler.ScheduledTaskHandler;
import com.icthh.xm.ms.scheduler.handler.ScheduledTaskHandlerImpl;
import com.icthh.xm.ms.scheduler.nameresolver.ChannelNameResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@ConditionalOnProperty("application.stream-binding-enabled")
public class MessagingConfiguration {

    @Bean
    public ScheduledTaskHandler scheduledTaskHandler(BinderAwareChannelResolver channelResolver,
                                                     ChannelNameResolver nameResolver) {
        return new ScheduledTaskHandlerImpl(channelResolver, nameResolver);
    }

}
