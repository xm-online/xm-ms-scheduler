package com.icthh.xm.ms.scheduler.nameresolver;

import com.icthh.xm.ms.scheduler.AbstractSpringBootTest;
import com.icthh.xm.ms.scheduler.domain.enumeration.ChannelType;
import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.icthh.xm.ms.scheduler.TaskTestUtil.TEST_TENANT;
import static com.icthh.xm.ms.scheduler.TaskTestUtil.createTaskFixedDelay;
import static org.junit.Assert.assertEquals;

public class TenantAwareChannelNameResolverTest extends AbstractSpringBootTest {

    @Autowired
    ChannelNameResolver nameResolver;

    @Test
    public void testResolveChannelName() {

        TaskDTO task = createTaskFixedDelay(1000L, null, null);
        task.setChannelType(null);
        task.setTenant(TEST_TENANT);

        assertEquals("scheduler_test_queue", nameResolver.resolve(task));

        task.setChannelType(ChannelType.TOPIC);
        assertEquals("scheduler_test_topic", nameResolver.resolve(task));

        task.setChannelType(ChannelType.QUEUE);
        assertEquals("scheduler_test_queue", nameResolver.resolve(task));

        task.setTargetMs("entity");
        assertEquals("scheduler_test_entity_queue", nameResolver.resolve(task));
    }

}
