package com.icthh.xm.ms.scheduler.config;

import com.icthh.xm.ms.scheduler.handler.ScheduledTaskHandler;
import com.icthh.xm.ms.scheduler.handler.ScheduledTaskHandlerImpl;
import com.icthh.xm.ms.scheduler.nameresolver.ChannelNameResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@EnableBinding
@ConditionalOnProperty("application.stream-binding-enabled")
@RequiredArgsConstructor
public class MessagingConfiguration {

    private static final String DEFAULT_SCHEDULER_QUEUE = "scheduler_xm_queue";

    @Bean
    public ScheduledTaskHandler scheduledTaskHandler(BinderAwareChannelResolver channelResolver,
                                                     ChannelNameResolver nameResolver) {
        // resolve destination for force init binding health check
        channelResolver.resolveDestination(DEFAULT_SCHEDULER_QUEUE);
        return new ScheduledTaskHandlerImpl(channelResolver, nameResolver);
    }

}
