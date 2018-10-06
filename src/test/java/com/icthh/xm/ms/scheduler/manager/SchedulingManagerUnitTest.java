package com.icthh.xm.ms.scheduler.manager;

import static com.icthh.xm.ms.scheduler.TaskTestUtil.*;
import static org.junit.Assert.assertEquals;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.internal.DefaultTenantContextHolder;
import com.icthh.xm.ms.scheduler.AbstractSpringContextTest;
import com.icthh.xm.ms.scheduler.handler.ScheduledTaskHandler;
import com.icthh.xm.ms.scheduler.service.ConfigTaskService;
import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Instant;
import java.util.Arrays;

/**
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class SchedulingManagerUnitTest extends AbstractSpringContextTest {

    private Multiset<Long> expiredTasks = HashMultiset.create();
    private Multiset<Long> executedTasks = HashMultiset.create();

    private SchedulingManager schedulingManager;

    @Autowired
    private ScheduledTaskHandler handler;

    @Autowired
    private ConfigTaskService configTaskService;

    private TenantContextHolder tenantContextHolder;

//    @Autowired
//    private PrivilegedTenantContext privilegedTenantContext;

    @Autowired
    private TenantListRepository tenantListRepository;

    // TODO - FIXME - task mapper does not autowired from IDE... need to configure Mapstruct annotation processors!
//    @Autowired
//    private TaskMapper taskMapper;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        tenantContextHolder = new DefaultTenantContextHolder();
        TenantContextUtils.setTenant(tenantContextHolder, XM_TENANT);

        schedulingManager = new SchedulingManager(tenantContextHolder, taskScheduler, configTaskService, handler,
                                                  executed -> executedTasks.add(executed.getId()),
                                                  expired -> expiredTasks.add(expired.getId()),
                                                  tenantListRepository);
    }

    @Test
    public void testInitFixedDelayTasks() {

        TaskDTO task = createTaskFixedDelay(300L, null, null);

        initScheduling(task);

        waitAndDeleteTask(300 * 5, task);

        expectRunAndExpiryCounts(task, 5, 0);

    }

    @Test
    public void testInitFixedRateTasks() {

        TaskDTO task = createTaskFixedRate(100L, null, null);

        initScheduling(task);

        // due to fixed rate we need to delete task little bit earlier
        waitAndDeleteTask(495, task);

        expectRunAndExpiryCounts(task, 5, 0);

    }

    @Test
    public void testInitCronTasks() {

        TaskDTO task = createTaskByCron("0/1 * * * * ?", null, null);

        initScheduling(task);

        // due to fixed rate we need to delete task little bit earlier
        waitAndDeleteTask(4000, task);

        expectRunAndExpiryCounts(task, 4, 0);

    }

    @Test
    public void testInitFixedDelayTasksWithExpiration() {

        TaskDTO task = createTaskFixedDelay(500L, null, Instant.now().plusSeconds(2));

        initScheduling(task);

        waitAndDeleteTask(3000, task);

        expectRunAndExpiryCounts(task, 4, 1);

    }

    @Test
    public void testInitFixedDelayTasksWithInitialDelayAndExpiration() {

        TaskDTO task = createTaskFixedDelay(500L, Instant.now().plusSeconds(1), Instant.now().plusSeconds(2));

        initScheduling(task);

        waitAndDeleteTask(3500, task);

        expectRunAndExpiryCounts(task, 2, 1);

    }

    @Test
    public void testInitConcurrentTasksWithExpiration() {

        TaskDTO task1 = createTaskFixedDelay(500L, null, Instant.now().plusSeconds(2));
        TaskDTO task2 = createTaskFixedDelay(500L, null, Instant.now().plusSeconds(2));

        initScheduling(task1, task2);

        waitAndDeleteTask(3005, task1, task2);

        expectRunAndExpiryCounts(task1, 4, 1);
        expectRunAndExpiryCounts(task2, 4, 1);

    }

    // FIXME - can not autowire service
//    @Test
//    public void testInitFixedDelayTasksFromConfig() {
//
//        when(taskServiceExt.findAllNotFinishedTasks()).thenReturn(asList());
//        when(taskServiceExt.findAllNotFinishedTasks()).thenReturn(asList());
//
//        schedulingManager.init();
//
//        waitAndDeleteTask(3000);
//
//        TaskDTO task = taskServiceExt.findAllNotFinishedTaskFromConfig().get(0);
//
//        expectRunAndExpiryCounts(task, 3, 0);
//
//    }

    @Test
    public void testCreateTask() {

        TaskDTO task1 = createTaskFixedDelay(500L, null, Instant.now().plusSeconds(2));

        initScheduling();

        schedulingManager.updateActiveTask(task1);

        waitAndDeleteTask(3000, task1);

        expectRunAndExpiryCounts(task1, 4, 1);

    }

    @Test
    public void testUpdateTask() {

        TaskDTO task1 = createTaskFixedDelay(500L, Instant.now().plusMillis(20), null);

        initScheduling();

        // create task
        schedulingManager.updateActiveTask(task1);

        waitFor(1990);
        expectRunAndExpiryCounts(task1, 4, 0);

        // update existing task
        task1.setDelay(250L);
        task1.setEndDate(Instant.now().plusSeconds(2));
        schedulingManager.updateActiveTask(task1);

        waitFor(2000);
        expectRunAndExpiryCounts(task1, 4 + 8, 1);

    }

    private void waitAndDeleteTask(long wait, TaskDTO... tasks) {
        waitFor(wait);

        Arrays.stream(tasks).forEach(task -> {
            schedulingManager.deleteActiveTask(task.getId().toString());
        });

    }

    private void initScheduling(TaskDTO... tasks) {
        // TODO - fixme - mock on Repository level instead of service ti test service logic (impossible due to IDE
        // does not recognise Mapstruct generated code)
        Arrays.stream(tasks).forEach(schedulingManager::updateActiveTask);
    }

    private void expectRunAndExpiryCounts(TaskDTO task, int runCount, int expirycount) {
        assertEquals(runCount, executedTasks.count(task.getId()));
        assertEquals(expirycount, expiredTasks.count(task.getId()));
    }

}
