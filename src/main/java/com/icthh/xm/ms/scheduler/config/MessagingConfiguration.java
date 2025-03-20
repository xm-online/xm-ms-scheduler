package com.icthh.xm.ms.scheduler.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.topic.service.KafkaTemplateService;
import com.icthh.xm.ms.scheduler.handler.ScheduledTaskHandler;
import com.icthh.xm.ms.scheduler.handler.ScheduledTaskHandlerImpl;
import com.icthh.xm.ms.scheduler.nameresolver.ChannelNameResolver;
import com.icthh.xm.ms.scheduler.nameresolver.TenantAwareChannelNameResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MessagingConfiguration {

    @Bean
    public ChannelNameResolver channelNameResolver() {
        return new TenantAwareChannelNameResolver();
    }

    @Bean
    public ScheduledTaskHandler scheduledTaskHandler(KafkaTemplateService kafkaTemplateService,
                                                     ChannelNameResolver nameResolver, ObjectMapper objectMapper) {

        return new ScheduledTaskHandlerImpl(kafkaTemplateService, nameResolver, objectMapper);
    }

}
