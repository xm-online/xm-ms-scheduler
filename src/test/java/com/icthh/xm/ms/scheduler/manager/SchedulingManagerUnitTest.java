package com.icthh.xm.ms.scheduler.manager;

import static com.icthh.xm.ms.scheduler.TaskTestUtil.XM_TENANT;
import static com.icthh.xm.ms.scheduler.TaskTestUtil.createTaskByCron;
import static com.icthh.xm.ms.scheduler.TaskTestUtil.createTaskFixedDelay;
import static com.icthh.xm.ms.scheduler.TaskTestUtil.createTaskOneTime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.internal.DefaultTenantContextHolder;
import com.icthh.xm.ms.scheduler.AbstractUnitTest;
import com.icthh.xm.ms.scheduler.domain.enumeration.ScheduleType;
import com.icthh.xm.ms.scheduler.handler.ScheduledTaskHandler;
import com.icthh.xm.ms.scheduler.repository.TaskRepository;
import com.icthh.xm.ms.scheduler.service.SystemTaskService;
import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

public class SchedulingManagerUnitTest extends AbstractUnitTest {

    private TenantContextHolder holder;
    private ThreadPoolTaskScheduler scheduler;
    private SystemTaskService systemTaskService;
    private TaskRepository taskRepository;
    private ScheduledTaskHandler handler;
    private final List<Long> handled = new CopyOnWriteArrayList<>();
    private SchedulingManager manager;

    private ListAppender<ILoggingEvent> logs;
    private Logger managerLogger;

    @BeforeEach
    public void setUp() {
        managerLogger = (Logger) LoggerFactory.getLogger(SchedulingManager.class);
        logs = new ListAppender<>();
        logs.start();
        managerLogger.addAppender(logs);
        holder = new DefaultTenantContextHolder();
        holder.getPrivilegedContext().destroyCurrentContext(); // a tenant can be set only once per thread
        TenantContextUtils.setTenant(holder, XM_TENANT);
        scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(3);
        scheduler.initialize();
        systemTaskService = mock(SystemTaskService.class);
        taskRepository = mock(TaskRepository.class);
        handler = task -> handled.add(task.getId());
        manager = new SchedulingManager(holder, scheduler, systemTaskService, handler,
            mock(TenantListRepository.class), taskRepository);
    }

    @AfterEach
    public void tearDown() {
        managerLogger.detachAppender(logs);
        manager.destroy();
        scheduler.shutdown();
    }

    @Test
    public void doneIsCommittedBeforeTaskLeavesTheMap() {
        TaskDTO task = createTaskOneTime(Instant.now().plusMillis(200), 30);
        task.setEndDate(Instant.now().plusSeconds(30));
        AtomicBoolean stillScheduledDuringDone = new AtomicBoolean();
        AtomicBoolean doneCalled = new AtomicBoolean();
        when(taskRepository.updateStateKey(anyLong(), any())).thenAnswer(inv -> {
            // the scheduler thread has no tenant of its own: read the map in the task's tenant
            TenantContextUtils.setTenant(holder, XM_TENANT);
            stillScheduledDuringDone.set(manager.getActiveUserTaskKeys().contains(String.valueOf(task.getId())));
            doneCalled.set(true);
            return 1;
        });

        manager.createOrUpdateActiveUserTask(task);

        await().atMost(Duration.ofSeconds(5)).untilTrue(doneCalled);
        await().atMost(Duration.ofSeconds(5)).until(() -> {
            TenantContextUtils.setTenant(holder, XM_TENANT);
            return manager.getActiveUserTaskKeys().isEmpty();
        });
        assertThat(stillScheduledDuringDone).isTrue();
    }

    @Test
    public void schedulerNeverWritesEndDateOfAOneTimeTaskThatHasOne() {
        TaskDTO task = createTaskOneTime(Instant.now().plusSeconds(60), 30);
        task.setEndDate(Instant.now().plusSeconds(90));

        manager.createOrUpdateActiveUserTask(task);

        verify(taskRepository, never()).findById(any());
        verify(taskRepository, never()).save(any());
        verify(taskRepository, never()).saveAndFlush(any());
        verify(taskRepository, never()).backfillEndDate(anyLong(), any());
    }

    @Test
    public void nullEndDateIsBackfilledByATargetedUpdateOnly() {
        Instant start = Instant.now().plusSeconds(60);
        TaskDTO task = createTaskOneTime(start, 30);

        manager.createOrUpdateActiveUserTask(task);

        verify(taskRepository).backfillEndDate(task.getId(), start.plusSeconds(30));
        verify(taskRepository, never()).save(any());
        verify(taskRepository, never()).findById(any());
    }

    @Test
    public void setStateUsesTargetedUpdate() {
        TaskDTO task = createTaskOneTime(Instant.now(), 30);
        task.setTenant(XM_TENANT);
        manager.setState(com.icthh.xm.ms.scheduler.domain.enumeration.StateKey.DONE, task);
        verify(taskRepository).updateStateKey(task.getId(), "DONE");
        verify(taskRepository, never()).save(any());
    }

    @Test
    public void scheduleQuietlyContinuesAfterInvalidRow() {
        TaskDTO bad = createTaskByCron("not a cron", null, null);
        TaskDTO good = createTaskFixedDelay(60_000L, Instant.now().plusSeconds(60), null);
        TaskDTO system = createTaskFixedDelay(60_000L, Instant.now().plusSeconds(60), null);
        system.setId(null);
        system.setKey("sys-1");

        SchedulingManager.ScheduleCounts counts = manager.scheduleQuietly(List.of(bad, good), List.of(system));

        assertThat(counts.invalid()).isEqualTo(1);
        assertThat(counts.scheduled()).isEqualTo(2);
        assertThat(manager.getActiveUserTaskKeys()).containsExactly(String.valueOf(good.getId()));
        assertThat(manager.getActiveSystemTaskKeys()).containsExactly("sys-1");
    }

    @Test
    public void cancelIfCancelsOnlyMatchingTasks() {
        TaskDTO a = createTaskFixedDelay(60_000L, Instant.now().plusSeconds(60), null);
        TaskDTO b = createTaskFixedDelay(60_000L, Instant.now().plusSeconds(60), null);
        manager.createOrUpdateActiveUserTask(a);
        manager.createOrUpdateActiveUserTask(b);

        int cancelled = manager.cancelIf(t -> t.getId().equals(a.getId()));

        assertThat(cancelled).isEqualTo(1);
        assertThat(manager.getActiveUserTaskKeys()).containsExactly(String.valueOf(b.getId()));
    }

    @Test
    public void initSkipsTheStartupLoadWhenOwnershipLoadsNothing() {
        TenantListRepository tenants = mock(TenantListRepository.class);
        TaskOwnership loadsNothing = new TaskOwnership() {
            public boolean owns(TaskDTO t) {
                return true;
            }

            public boolean loadsAllOnStartup() {
                return false;
            }

            public boolean beginFiring(TaskDTO t) {
                return true;
            }

            public void endFiring(TaskDTO t) {
            }
        };
        manager = new SchedulingManager(holder, scheduler, systemTaskService, handler,
            null, null, tenants, taskRepository, loadsNothing);

        manager.init();

        verify(tenants, never()).getTenants();
        assertThat(manager.getActiveUserTaskKeys()).isEmpty();
    }

    @Test
    public void notOwnedNowDoesNotFireAndDoesNotWriteDone() {
        TaskOwnership fenced = new TaskOwnership() {
            public boolean owns(TaskDTO t) {
                return true;
            }

            public boolean loadsAllOnStartup() {
                return true;
            }

            public boolean beginFiring(TaskDTO t) {
                return false;
            }

            public void endFiring(TaskDTO t) {
            }
        };
        manager = new SchedulingManager(holder, scheduler, systemTaskService, handler,
            null, null, mock(TenantListRepository.class), taskRepository, fenced);
        TaskDTO task = createTaskOneTime(Instant.now().plusMillis(100), 30);
        task.setEndDate(Instant.now().plusSeconds(30));
        manager.createOrUpdateActiveUserTask(task);

        await().pollDelay(Duration.ofMillis(600)).atMost(Duration.ofSeconds(3)).until(() -> true);

        assertThat(handled).isEmpty();
        verify(taskRepository, never()).updateStateKey(anyLong(), any());
    }

    @Test
    public void staleDtoDoesNotRemoveANewerSchedule() {
        TaskDTO first = createTaskFixedDelay(60_000L, Instant.now().plusSeconds(60), null);
        manager.createOrUpdateActiveUserTask(first);
        TaskDTO second = createTaskFixedDelay(30_000L, Instant.now().plusSeconds(60), null);
        second.setId(first.getId());
        manager.createOrUpdateActiveUserTask(second);

        manager.deleteExpiredTask(first);

        assertThat(manager.getActiveUserTaskKeys()).containsExactly(String.valueOf(first.getId()));
        assertThat(manager.findScheduledUserTask(String.valueOf(first.getId()))).containsSame(second);
        // the user map holds a newer task: it logs nothing; only the system map (no such key) logs master's line
        assertThat(infoMessages()).filteredOn(m -> m.contains("removed, cancel result"))
            .containsExactly("task [" + first.getId() + "] removed, cancel result: false");
    }

    @Test
    public void expiredTaskLogsMasterLinePerMap() {
        TaskDTO task = createTaskFixedDelay(60_000L, Instant.now().plusSeconds(60), null);
        manager.createOrUpdateActiveUserTask(task);
        logs.list.clear();

        manager.deleteExpiredTask(task);

        String key = String.valueOf(task.getId());
        assertThat(manager.getActiveUserTaskKeys()).isEmpty();
        assertThat(infoMessages()).containsExactly("task [" + key + "] removed, cancel result: true",
            "task [" + key + "] removed, cancel result: false");
    }

    @Test
    public void masterUpdateLinesUnchanged() {
        TaskDTO task = createTaskFixedDelay(60_000L, Instant.now().plusSeconds(60), null);

        manager.createOrUpdateActiveUserTask(task);
        manager.createOrUpdateActiveUserTask(task);

        String key = String.valueOf(task.getId());
        assertThat(infoMessages()).containsExactly("create new task during update: " + key,
            "cancel active task: " + key + " for update. cancelled result: true");
    }

    @Test
    public void scheduleQuietlyDoesNotLog() {
        TaskDTO b = createTaskFixedDelay(60_000L, Instant.now().plusSeconds(60), null);

        manager.scheduleQuietly(List.of(b), List.of());

        assertThat(logs.list).noneMatch(e -> e.getLevel().isGreaterOrEqual(Level.INFO));
    }

    private List<String> infoMessages() {
        return logs.list.stream().filter(e -> e.getLevel() == Level.INFO).map(ILoggingEvent::getFormattedMessage)
            .toList();
    }

    @Test
    public void systemTasksAreFilteredByOwnership() {
        TaskOwnership none = new TaskOwnership() {
            public boolean owns(TaskDTO t) {
                return t.getKey().startsWith("mine");
            }

            public boolean loadsAllOnStartup() {
                return true;
            }

            public boolean beginFiring(TaskDTO t) {
                return true;
            }

            public void endFiring(TaskDTO t) {
            }
        };
        manager = new SchedulingManager(holder, scheduler, systemTaskService, handler,
            null, null, mock(TenantListRepository.class), taskRepository, none);
        TaskDTO mine = createTaskFixedDelay(60_000L, Instant.now().plusSeconds(60), null);
        mine.setId(null);
        mine.setKey("mine-1");
        TaskDTO other = createTaskFixedDelay(60_000L, Instant.now().plusSeconds(60), null);
        other.setId(null);
        other.setKey("other-1");
        when(systemTaskService.findSystemNotFinishedTasks()).thenReturn(List.of(mine, other));

        manager.mergeSystemTasksFromConfig(XM_TENANT);

        assertThat(manager.getActiveSystemTaskKeys()).containsExactly("mine-1");
        assertThat(ScheduleType.FIXED_DELAY).isEqualTo(mine.getScheduleType());
    }
}
