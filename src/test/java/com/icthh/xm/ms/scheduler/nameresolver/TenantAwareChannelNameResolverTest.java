package com.icthh.xm.ms.scheduler.nameresolver;

import static com.icthh.xm.ms.scheduler.TaskTestUtil.createTaskFixedDelay;
import static org.junit.Assert.assertEquals;

import com.icthh.xm.ms.scheduler.config.MessagingConfiguration;
import com.icthh.xm.ms.scheduler.domain.enumeration.ChannelType;
import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {MessagingConfiguration.class})
public class TenantAwareChannelNameResolverTest {

    @Autowired
    ChannelNameResolver nameResolver;

    @Test
    public void testResolveChannelName() {

        TaskDTO task = createTaskFixedDelay(1000L, null, null);
        task.setChannelType(null);

        assertEquals("scheduler_queue", nameResolver.resolve(task));

        task.setChannelType(ChannelType.TOPIC);
        assertEquals("scheduler_topic", nameResolver.resolve(task));

        task.setChannelType(ChannelType.QUEUE);
        assertEquals("scheduler_queue", nameResolver.resolve(task));

    }

}
