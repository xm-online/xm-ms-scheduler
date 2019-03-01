package com.icthh.xm.ms.scheduler.manager;

import static com.icthh.xm.ms.scheduler.TaskTestUtil.XM_TENANT;
import static com.icthh.xm.ms.scheduler.TaskTestUtil.createTaskByCron;
import static com.icthh.xm.ms.scheduler.TaskTestUtil.createTaskOneTime;
import static com.icthh.xm.ms.scheduler.TaskTestUtil.createTaskFixedDelay;
import static com.icthh.xm.ms.scheduler.TaskTestUtil.createTaskFixedRate;
import static com.icthh.xm.ms.scheduler.TaskTestUtil.waitFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.internal.DefaultTenantContextHolder;
import com.icthh.xm.ms.scheduler.AbstractSpringContextTest;
import com.icthh.xm.ms.scheduler.SchedulerApp;
import com.icthh.xm.ms.scheduler.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.ms.scheduler.config.tenant.WebappTenantOverrideConfiguration;
import com.icthh.xm.ms.scheduler.domain.Task;
import com.icthh.xm.ms.scheduler.domain.enumeration.StateKey;
import com.icthh.xm.ms.scheduler.handler.ScheduledTaskHandler;
import com.icthh.xm.ms.scheduler.repository.TaskRepository;
import com.icthh.xm.ms.scheduler.service.SystemTaskService;
import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    SchedulerApp.class,
    SecurityBeanOverrideConfiguration.class,
    WebappTenantOverrideConfiguration.class
})
public class SchedulingManagerUnitTest extends AbstractSpringContextTest {

    private Multiset<Long> expiredTasks = HashMultiset.create();
    private Multiset<Long> executedTasks = HashMultiset.create();

    private SchedulingManager schedulingManager;

    @Autowired
    private ScheduledTaskHandler handler;

    @Mock
    private SystemTaskService systemTaskService;

    private TenantContextHolder tenantContextHolder;

    @Autowired
    private TenantListRepository tenantListRepository;

    @Mock
    private TaskRepository taskRepository;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        tenantContextHolder = new DefaultTenantContextHolder();
        TenantContextUtils.setTenant(tenantContextHolder, XM_TENANT);

        schedulingManager = new SchedulingManager(tenantContextHolder, taskScheduler, systemTaskService, handler,
                                                  executed -> executedTasks.add(executed.getId()),
                                                  expired -> expiredTasks.add(expired.getId()),
                                                  tenantListRepository, taskRepository);
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
    public void testInitCronTasksWithInitialDelayAndExpiration() {

        Instant nearestSecond = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        TaskDTO task = createTaskByCron("0/1 * * * * ?", nearestSecond.plusSeconds(1), nearestSecond.plusSeconds(2));

        initScheduling(task);

        waitAndDeleteTask(4000, task);

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


    @Test
    public void testCreateTask() {

        TaskDTO task1 = createTaskFixedDelay(500L, null, Instant.now().plusSeconds(2));

        initScheduling();

        schedulingManager.createOrUpdateActiveUserTask(task1);

        waitAndDeleteTask(3000, task1);

        expectRunAndExpiryCounts(task1, 4, 1);

    }

    @Test
    public void testUpdateTask() {

        TaskDTO task1 = createTaskFixedDelay(500L, Instant.now().plusMillis(20), null);

        initScheduling();

        // create task
        schedulingManager.createOrUpdateActiveUserTask(task1);

        waitFor(1990);
        expectRunAndExpiryCounts(task1, 4, 0);

        // update existing task
        task1.setDelay(250L);
        task1.setEndDate(Instant.now().plusSeconds(2));
        schedulingManager.createOrUpdateActiveUserTask(task1);

        waitFor(2000);
        expectRunAndExpiryCounts(task1, 4 + 8, 1);

    }

    @Test
    public void testInitOneTimeTasks() {

        TaskDTO task = createTaskOneTime(Instant.now().plusMillis(1000), 3);

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(new Task()));
        when(taskRepository.save(any())).thenReturn(new Task());

        initScheduling(task);

        // due to fixed rate we need to delete task little bit earlier
        waitFor(2000);

        expectRunAndExpiryCounts(task, 1, 1);

        Optional<Task> stored = taskRepository.findById(task.getId());
        assertTrue(stored.isPresent());
        assertEquals(stored.get().getStateKey(), StateKey.DONE.name());
    }

    @Test
    public void testInitOneTimeExpiredEndDateTasks() {

        TaskDTO task = createTaskOneTime(Instant.now().minusMillis(1000), 3);

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(new Task()));
        when(taskRepository.save(any())).thenReturn(new Task());

        initScheduling(task);

        // due to fixed rate we need to delete task little bit earlier
        waitFor(1000);

        expectRunAndExpiryCounts(task, 1, 1);
    }

    @Test
    public void testInitOneTimeExpiredTasks() {

        TaskDTO task = createTaskOneTime(Instant.now().minusMillis(5000), 3);

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(new Task()));
        when(taskRepository.save(any())).thenReturn(new Task());

        initScheduling(task);

        // due to fixed rate we need to delete task little bit earlier
        waitFor(1000);

        expectRunAndExpiryCounts(task, 0, 1);

    }

    private void waitAndDeleteTask(long wait, TaskDTO... tasks) {
        waitFor(wait);

        Arrays.stream(tasks).forEach(task -> {
            schedulingManager.deleteActiveTask(task.getId().toString());
        });

    }

    private void initScheduling(TaskDTO... tasks) {
        List taskList = Arrays.asList(tasks);

        Answer<List<TaskDTO>> answer = invocation -> {
            if (TenantContextUtils.getRequiredTenantKey(tenantContextHolder).getValue().equals(XM_TENANT)) {
                return taskList;
            }
            return Collections.emptyList();
        };

        // There are two tenants in the Scope, so we need to init tasks only for XM tenant.
        Mockito.when(systemTaskService.findUserNotFinishedTasks()).thenAnswer(answer);
        schedulingManager.destroy();
        schedulingManager.init();
    }

    private void expectRunAndExpiryCounts(TaskDTO task, int runCount, int expirycount) {
        assertEquals(runCount, executedTasks.count(task.getId()));
        assertEquals(expirycount, expiredTasks.count(task.getId()));
    }

}
