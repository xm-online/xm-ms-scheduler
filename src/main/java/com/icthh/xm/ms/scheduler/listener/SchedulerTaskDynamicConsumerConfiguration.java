package com.icthh.xm.ms.scheduler.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.topic.domain.DynamicConsumer;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import com.icthh.xm.commons.topic.service.DynamicConsumerConfiguration;
import com.icthh.xm.ms.scheduler.service.TaskService;
import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.icthh.xm.commons.tenant.TenantContextUtils.buildTenant;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "application.scheduler-task-consumer.enabled", havingValue = "false", matchIfMissing = true)
public class SchedulerTaskDynamicConsumerConfiguration implements DynamicConsumerConfiguration {

    private final SchedulerTaskConsumerRefreshableConfiguration consumerConfiguration;
    private final TaskService taskService;
    private final TenantContextHolder tenantContextHolder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<DynamicConsumer> getDynamicConsumers(String tenantKey) {
        TopicConfig topicConfig = consumerConfiguration.getTopicConfigs().getOrDefault(tenantKey, defaultTopicConfig());
        specifyTopicName(tenantKey, topicConfig);
        DynamicConsumer dynamicConsumer = new DynamicConsumer();
        dynamicConsumer.setConfig(topicConfig);
        dynamicConsumer.setMessageHandler((message, tenant, topic) -> {
            tenantContextHolder.getPrivilegedContext().execute(buildTenant(tenant), () -> {
                handleMessage(message);
            });
        });
        return List.of(dynamicConsumer);
    }

    @SneakyThrows
    private void handleMessage(String message) {
        TaskDTO taskDto = objectMapper.readValue(message, TaskDTO.class);
        taskService.save(taskDto);
    }

    private void specifyTopicName(String tenantKey, TopicConfig topicConfig) {
        topicConfig.setGroupId("scheduler");
        topicConfig.setTopicName(tenantKey + ".scheduler-tasks");
        topicConfig.setTypeKey(tenantKey + ".scheduler-tasks");
        topicConfig.setDeadLetterQueue(tenantKey + ".scheduler-tasks-dead-letter");
    }

    private TopicConfig defaultTopicConfig() {
        TopicConfig topicConfig = new TopicConfig();
        topicConfig.setBackOffPeriod(30_000L);
        topicConfig.setRetriesCount(3);
        return topicConfig;
    }
}
