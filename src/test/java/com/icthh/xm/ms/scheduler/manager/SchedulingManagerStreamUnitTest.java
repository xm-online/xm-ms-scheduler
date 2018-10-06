package com.icthh.xm.ms.scheduler.manager;

import static com.icthh.xm.ms.scheduler.TaskTestUtil.TEST_TENANT;
import static com.icthh.xm.ms.scheduler.TaskTestUtil.createTaskFixedDelay;
import static com.icthh.xm.ms.scheduler.TaskTestUtil.waitFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.ms.scheduler.config.MessagingConfiguration;
import com.icthh.xm.ms.scheduler.config.SchedulingHandlerConfiguration;
import com.icthh.xm.ms.scheduler.domain.ScheduledEvent;
import com.icthh.xm.ms.scheduler.handler.ScheduledTaskHandler;
import com.icthh.xm.ms.scheduler.service.SystemTaskService;
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
import java.util.Optional;

/**
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    TestSupportBinderAutoConfiguration.class,
    TestSchedulingManagerConfiguration.class,
    MessagingConfiguration.class,
    SchedulingHandlerConfiguration.class
}, properties = {"application.stream-binding-enabled=true"})
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
    private TenantContextHolder tenantContextHolder;

    @Mock
    private TenantContext tenantContext;

    @Mock
    private TenantListRepository tenantListRepository;

    @Mock
    private SystemTaskService systemTaskService;

    @Before
    public void init() {

        MockitoAnnotations.initMocks(this);

        // clear messages
        nameResolver.getResolvedChannels()
                    .forEach(s -> messageCollector.forChannel(channelResolver.resolveDestination(s)).clear());

        schedulingManager = new SchedulingManager(tenantContextHolder, taskScheduler,
                                                  systemTaskService, handler, tenantListRepository);

        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf(TEST_TENANT)));
        when(tenantContextHolder.getContext()).thenReturn(tenantContext);
    }

    @Test
    public void testMessagesSentToChannel() {

        TaskDTO task = createTaskFixedDelay(1000L, Instant.now(), null);

        schedulingManager.updateOrUpdateActiveTask(task);

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
