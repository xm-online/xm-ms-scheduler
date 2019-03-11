package com.icthh.xm.ms.scheduler.config;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

import com.icthh.xm.ms.scheduler.handler.ScheduledTaskHandler;
import com.icthh.xm.ms.scheduler.handler.ScheduledTaskHandlerImpl;
import com.icthh.xm.ms.scheduler.nameresolver.ChannelNameResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.binder.DefaultBinderFactory.Listener;
import org.springframework.cloud.stream.binder.kafka.KafkaBinderHealthIndicator;
import org.springframework.cloud.stream.binder.kafka.KafkaMessageChannelBinder;
import org.springframework.cloud.stream.binder.kafka.config.KafkaBinderConfiguration;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;

/**
 * Configures Spring Cloud Stream support.
 *
 * See http://docs.spring.io/spring-cloud-stream/docs/current/reference/htmlsingle/
 * for more information.
 */
@Slf4j
@EnableBinding
@ConditionalOnProperty("application.stream-binding-enabled")
@Import({KafkaBinderConfiguration.class})
@RequiredArgsConstructor
public class MessagingConfiguration {

    private final KafkaBinderHealthIndicator kafkaBinderHealthIndicator;
    private final KafkaMessageChannelBinder binder;

    @Bean
    public ScheduledTaskHandler scheduledTaskHandler(BinderAwareChannelResolver channelResolver,
                                                     ChannelNameResolver nameResolver) {
        return new ScheduledTaskHandlerImpl(channelResolver, nameResolver);
    }

    @Bean
    @Order(HIGHEST_PRECEDENCE)
    public Listener healthCheckInit() {
        return (name, context) -> {
            registerBean(context, kafkaBinderHealthIndicator);
            // TODO in this case check message was sended
        };
    }

    private void registerBean(ApplicationContext context, HealthIndicator healthIndicator) {
        ((AnnotationConfigApplicationContext) context)
            .registerBean(healthIndicator.getClass(), healthIndicator);
    }

}
