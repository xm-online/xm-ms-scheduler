package com.icthh.xm.ms.scheduler.config;

import com.icthh.xm.ms.scheduler.manager.ChannelNameResolver;
import com.icthh.xm.ms.scheduler.manager.TenantAwareChannelNameResolver;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.context.annotation.Bean;

/**
 * Configures Spring Cloud Stream support.
 *
 * See http://docs.spring.io/spring-cloud-stream/docs/current/reference/htmlsingle/
 * for more information.
 */
// TODO - uncoment to test kafka messages
//@EnableBinding(value = {Source.class})
@EnableBinding
public class MessagingConfiguration {

    @Bean
    public ChannelNameResolver channelNameResolver() {
        return new TenantAwareChannelNameResolver();
    }

}
