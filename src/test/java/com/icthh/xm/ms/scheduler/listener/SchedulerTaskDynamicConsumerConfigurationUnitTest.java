package com.icthh.xm.ms.scheduler.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.tenant.internal.DefaultTenantContextHolder;
import com.icthh.xm.commons.topic.domain.DynamicConsumer;
import com.icthh.xm.ms.scheduler.AbstractUnitTest;
import com.icthh.xm.ms.scheduler.service.TaskService;
import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.databind.json.JsonMapper;

public class SchedulerTaskDynamicConsumerConfigurationUnitTest extends AbstractUnitTest {

    @Test
    public void timeZoneOfATaskFromTheTopicIsDroppedBeforeSave() {
        SchedulerTaskConsumerRefreshableConfiguration refreshable =
            mock(SchedulerTaskConsumerRefreshableConfiguration.class);
        when(refreshable.getTopicConfigs()).thenReturn(Map.of());
        TaskService taskService = mock(TaskService.class);
        SchedulerTaskDynamicConsumerConfiguration configuration = new SchedulerTaskDynamicConsumerConfiguration(
            refreshable, taskService, new DefaultTenantContextHolder(), JsonMapper.builder().build(),
            mock(ApplicationEventPublisher.class), mock(TenantListRepository.class));
        DynamicConsumer consumer = configuration.getDynamicConsumers("XM").get(0);

        consumer.getMessageHandler().onMessage("{\"key\":\"k\",\"typeKey\":\"T\",\"createdBy\":\"u\","
            + "\"scheduleType\":\"CRON\",\"cronExpression\":\"0 0 * * * ?\","
            + "\"cronTriggerTimeZoneId\":\"Europe/Kyiv\"}", "XM", consumer.getConfig());

        ArgumentCaptor<TaskDTO> saved = ArgumentCaptor.forClass(TaskDTO.class);
        verify(taskService).save(saved.capture());
        assertThat(saved.getValue().getCronTriggerTimeZoneId()).isNull();
    }
}
