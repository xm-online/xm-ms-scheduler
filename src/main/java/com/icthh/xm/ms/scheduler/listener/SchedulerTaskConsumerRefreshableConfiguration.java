package com.icthh.xm.ms.scheduler.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import com.icthh.xm.commons.topic.service.dto.RefreshDynamicConsumersEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulerTaskConsumerRefreshableConfiguration implements RefreshableConfiguration {

    private static final String TENANT_NAME = "tenantName";
    private static final String FILE_PATTERN = "/config/tenants/{tenantName}/scheduler/tasks-consumer.yml";
    private final AntPathMatcher matcher = new AntPathMatcher();
    private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    private final ApplicationEventPublisher eventPublisher;

    @Getter
    private final Map<String, TopicConfig> topicConfigs = new ConcurrentHashMap<>();


    @Override
    public void onRefresh(String updatedKey, String config) {
        try {
            String tenant = extractTenant(updatedKey);
            if (StringUtils.isBlank(config)) {
                topicConfigs.remove(updatedKey);
                log.info("Scheduler task consumer config for tenant '{}' were removed: {}", tenant, updatedKey);
            } else {
                TopicConfig spec = mapper.readValue(config, TopicConfig.class);
                topicConfigs.put(updatedKey, spec);
                log.info("Scheduler task consumer config for tenant '{}' were updated: {}", tenant, updatedKey);
            }
            eventPublisher.publishEvent(new RefreshDynamicConsumersEvent(this, tenant));
        } catch (Exception e) {
            log.error("Error read scheduler task consumer config from path: {}", updatedKey, e);
        }
    }

    private String extractTenant(final String updatedKey) {
        return matcher.extractUriTemplateVariables(FILE_PATTERN, updatedKey).get(TENANT_NAME);
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return matcher.match(FILE_PATTERN, updatedKey);
    }
}
