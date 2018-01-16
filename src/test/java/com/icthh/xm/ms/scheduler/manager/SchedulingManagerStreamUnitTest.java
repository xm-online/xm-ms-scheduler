package com.icthh.xm.ms.scheduler.manager;

import static com.icthh.xm.ms.scheduler.TaskTestUtil.createTaskFixedDelay;
import static com.icthh.xm.ms.scheduler.TaskTestUtil.waitFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.icthh.xm.ms.scheduler.config.MessagingConfiguration;
import com.icthh.xm.ms.scheduler.domain.ScheduledEvent;
import com.icthh.xm.ms.scheduler.handler.ScheduledTaskHandler;
import com.icthh.xm.ms.scheduler.service.TaskServiceExt;
import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.cloud.stream.test.binder.TestSupportBinderAutoConfiguration;
import org.springframework.messaging.Message;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

/**
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    TestSupportBinderAutoConfiguration.class,
    TestSchedulingManagerConfiguration.class,
    MessagingConfiguration.class
})
//@ContextHierarchy({
//    @ContextConfiguration(classes = TestSupportBinderAutoConfiguration.class),
//    @ContextConfiguration(classes = MessagingConfiguration.class),
//    @ContextConfiguration(classes = TestSchedulingManagerConfiguration.class)
//})
public class SchedulingManagerStreamUnitTest {

    private SchedulingManager schedulingManager;

    @Autowired
    private ScheduledTaskHandler handler;

    @Autowired
    private BinderAwareChannelResolver channelResolver;

    @Autowired
    private TestChannelNameResolver nameResolver;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    @Autowired
    private MessageCollector messageCollector;

    @Mock
    private TaskServiceExt taskServiceExt;

    @Before
    public void init() {

        MockitoAnnotations.initMocks(this);

        // clear messages
        nameResolver.getResolvedChannels()
                    .forEach(s -> messageCollector.forChannel(channelResolver.resolveDestination(s)).clear());

        schedulingManager = new SchedulingManager(taskScheduler,
                                                  taskServiceExt,
                                                  handler);
    }

    @Test
    public void testMessagesSentToChannel() {

        TaskDTO task = createTaskFixedDelay(1000L, Instant.now(), null);

        schedulingManager.updateActiveTask(task);

        waitFor(3000);

        schedulingManager.deleteActiveTask(task.getId().toString());

        List<Message> messages = new LinkedList<>();

        messageCollector
            .forChannel(channelResolver.resolveDestination(nameResolver.resolve(task)))
            .drainTo(messages);

        assertEquals(3, messages.size());
        assertTrue(messages.stream().allMatch(m -> ((ScheduledEvent) m.getPayload()).getId().equals(task.getId())));

    }

}
