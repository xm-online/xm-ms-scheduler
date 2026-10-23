package com.icthh.xm.ms.scheduler.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.tenant.PrivilegedTenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.topic.domain.DynamicConsumer;
import com.icthh.xm.ms.scheduler.domain.enumeration.ScheduleType;
import com.icthh.xm.ms.scheduler.service.TaskService;
import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.databind.json.JsonMapper;

public class SchedulerTaskDynamicConsumerConfigurationUnitTest {

    @Test
    public void messageWithTimeZoneIsSavedWithoutZone() throws Exception {
        TaskService taskService = mock(TaskService.class);
        TenantContextHolder holder = mock(TenantContextHolder.class);
        PrivilegedTenantContext context = mock(PrivilegedTenantContext.class);
        Mockito.when(holder.getPrivilegedContext()).thenReturn(context);
        Mockito.doAnswer(i -> {
            ((Runnable) i.getArgument(1)).run();
            return null;
        }).when(context).execute(any(com.icthh.xm.commons.tenant.Tenant.class), any(Runnable.class));
        SchedulerTaskConsumerRefreshableConfiguration refreshable =
            mock(SchedulerTaskConsumerRefreshableConfiguration.class);
        Mockito.when(refreshable.getTopicConfigs()).thenReturn(Collections.emptyMap());
        JsonMapper mapper = JsonMapper.builder().build();
        SchedulerTaskDynamicConsumerConfiguration config = new SchedulerTaskDynamicConsumerConfiguration(
            refreshable, taskService, holder, mapper, mock(ApplicationEventPublisher.class),
            mock(TenantListRepository.class));

        TaskDTO task = new TaskDTO();
        task.setTypeKey("T");
        task.setScheduleType(ScheduleType.CRON);
        task.setCronExpression("0/30 * * * * ?");
        task.setCronTriggerTimeZoneId("Europe/Kyiv");
        DynamicConsumer consumer = config.getDynamicConsumers("XM").get(0);
        consumer.getMessageHandler().onMessage(mapper.writeValueAsString(task), "XM", consumer.getConfig());

        ArgumentCaptor<TaskDTO> saved = ArgumentCaptor.forClass(TaskDTO.class);
        verify(taskService).save(saved.capture());
        assertThat(saved.getValue().getCronTriggerTimeZoneId()).isNull();
    }
}
