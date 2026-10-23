package com.icthh.xm.ms.scheduler.manager;

import static com.icthh.xm.commons.tenant.TenantContextUtils.buildTenant;
import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.tenant.PrivilegedTenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.scheduler.domain.enumeration.StateKey;
import com.icthh.xm.ms.scheduler.handler.ScheduledTaskHandler;
import com.icthh.xm.ms.scheduler.repository.TaskRepository;
import com.icthh.xm.ms.scheduler.service.SystemTaskService;
import com.icthh.xm.ms.scheduler.service.TaskScheduleRules;
import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;

import java.math.BigInteger;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

/**
 * Scheduling manager component is designed to handle all active scheduled tasks.
 */
@Slf4j
public class SchedulingManager {

    private final TenantContextHolder tenantContextHolder;
    private final ThreadPoolTaskScheduler taskScheduler;
    private final SystemTaskService taskService;
    private final ScheduledTaskHandler handler;
    private final TenantListRepository tenantListRepository;
    private final TaskRepository taskRepository;
    private final TaskOwnership ownership;

    private Consumer<TaskDTO> afterRun;
    private Consumer<TaskDTO> afterExpiration;

    /**
     * A scheduled task: the DTO it was scheduled from and its future. {@code cancelledOld} is the cancel result of
     * the schedule this one replaced, null when it replaced nothing; the caller that scheduled it reads it once.
     */
    private record ScheduledTask(TaskDTO task, ScheduledFuture<?> future, Boolean cancelledOld) {

        ScheduledTask(TaskDTO task, ScheduledFuture<?> future) {
            this(task, future, null);
        }
    }

    /** Result of {@link #scheduleQuietly}: scheduled and invalid (skipped) tasks. */
    public record ScheduleCounts(int scheduled, int invalid) {
    }

    private final Map<String, Map<String, ScheduledTask>> systemSchedulers = new ConcurrentHashMap<>();
    private final Map<String, Map<String, ScheduledTask>> userSchedulers = new ConcurrentHashMap<>();

    /** The constructor. */
    public SchedulingManager(final TenantContextHolder tenantContextHolder,
                             final ThreadPoolTaskScheduler taskScheduler,
                             final SystemTaskService taskService,
                             final ScheduledTaskHandler handler,
                             final TenantListRepository tenantListRepository,
                             final TaskRepository taskRepository) {
        this(tenantContextHolder, taskScheduler, taskService, handler, null, null,
            tenantListRepository, taskRepository, new LocalOwnership());
    }

    /** The constructor. */
    public SchedulingManager(final TenantContextHolder tenantContextHolder,
                             final ThreadPoolTaskScheduler taskScheduler,
                             final SystemTaskService taskService,
                             final ScheduledTaskHandler handler,
                             final Consumer<TaskDTO> afterRun,
                             final Consumer<TaskDTO> afterExpiration,
                             final TenantListRepository tenantListRepository,
                             final TaskRepository taskRepository) {
        this(tenantContextHolder, taskScheduler, taskService, handler, afterRun, afterExpiration,
            tenantListRepository, taskRepository, new LocalOwnership());
    }

    /**
     * The constructor. The ownership decides which tasks this instance runs and whether startup loads every task
     * (see {@link TaskOwnership}).
     */
    public SchedulingManager(final TenantContextHolder tenantContextHolder,
                             final ThreadPoolTaskScheduler taskScheduler,
                             final SystemTaskService taskService,
                             final ScheduledTaskHandler handler,
                             final Consumer<TaskDTO> afterRun,
                             final Consumer<TaskDTO> afterExpiration,
                             final TenantListRepository tenantListRepository,
                             final TaskRepository taskRepository,
                             final TaskOwnership ownership) {
        this.tenantContextHolder = tenantContextHolder;
        this.taskScheduler = taskScheduler;
        this.taskService = taskService;
        this.handler = handler;
        this.tenantListRepository = tenantListRepository;
        this.taskRepository = taskRepository;
        this.ownership = ownership;
        this.afterRun = afterRun;
        this.afterExpiration = afterExpiration;
    }

    /**
     * Init.
     */
    public void init() {
        if (!ownership.loadsAllOnStartup()) {
            log.info("startup load skipped: the ownership loads the tasks later");
            return;
        }

        int taskCount = 0;

        log.info("Start init scheduled tasks");

        for (String tenantName : tenantListRepository.getTenants()) {
            // Translate tenant to uppercase as only tenant list repo contains lowercase value
            String tenant = StringUtils.upperCase(tenantName);
            PrivilegedTenantContext ptc = tenantContextHolder.getPrivilegedContext();
            taskCount += ptc.execute(buildTenant(tenant), () -> initInsideTenant(tenant));
        }

        log.info("Finish Scheduler initialization with [{}] tenants and [{}] active tasks",
            tenantListRepository.getTenants().size(), taskCount);
    }

    /**
     * Brings the scheduled system tasks of the tenant in line with the config: deletes the ones no longer
     * configured and schedules or updates the owned ones.
     */
    public void mergeSystemTasksFromConfig(final String tenant) {

        log.info("Merge system tasks");

        tenantContextHolder.getPrivilegedContext().execute(buildTenant(tenant), () -> {

            List<TaskDTO> newTasks = taskService.findSystemNotFinishedTasks();
            Map<String, TaskDTO> remainingTasks = newTasks.stream()
                .filter(this::isOwned)
                .collect(Collectors.toMap(SchedulingManager::getTaskKey,
                    Function.identity()));

            // delete redundant tasks
            Set<String> toDeleteKeys = getActiveSystemTaskKeys().stream()
                .filter(key -> !remainingTasks.containsKey(key))
                .collect(Collectors.toSet());
            toDeleteKeys.forEach(key -> deleteTaskFromTenant(tenant, key, systemSchedulers));

            // update remaining tasks
            remainingTasks.values().forEach(this::createOrUpdateActiveSystemTask);

        });

    }

    private Integer initInsideTenant(final String tenantName) {
        log.info("Start initialization tasks on behalf of tenant [{}]", tenantName);

        List<TaskDTO> systemTasks = taskService.findSystemNotFinishedTasks().stream()
            .filter(this::isOwned).collect(Collectors.toList());
        Integer sysTasks = initInsideTenant(tenantName, systemTasks, systemSchedulers);
        log.info("Finish tenant [{}] initialization with [{}] system active tasks", tenantName, sysTasks);

        Integer userTasks = initInsideTenant(tenantName, taskService.findUserNotFinishedTasks(), userSchedulers);
        log.info("Finish tenant [{}] initialization with [{}] user active tasks", tenantName, userTasks);

        return sysTasks + userTasks;
    }

    private Integer initInsideTenant(final String tenantName, List<TaskDTO> tasks,
                                     Map<String, Map<String, ScheduledTask>> schedulers) {

        //Synchronization is necessary because task can start earlier than its future is created in collection
        CountDownLatch latch = new CountDownLatch(BigInteger.ONE.intValue());
        Map<String, ScheduledTask> scheduledTasks = tasks
            .stream()
            .peek(task -> task.setTenant(tenantName))
            .collect(Collectors.toConcurrentMap(SchedulingManager::getTaskKey,
                taskDTO -> new ScheduledTask(taskDTO, this.schedule(taskDTO, latch))));

        schedulers.put(tenantName, scheduledTasks);
        latch.countDown();

        return tasks.size();
    }

    /** Cancels and forgets every scheduled task of all tenants. */
    public void destroy() {
        log.info("destroy task scheduler...");
        long cnt = destroy(userSchedulers);
        log.info("cancelled user tasks count: {}", cnt);
        cnt = destroy(systemSchedulers);
        log.info("cancelled system tasks count: {}", cnt);
    }

    private long destroy(Map<String, Map<String, ScheduledTask>> schedulers) {
        long cnt = schedulers.values().stream().flatMap(m -> m.values().stream())
            .map(st -> cancelTask(st.future())).count();
        schedulers.clear();
        return cnt;
    }

    /**
     * Schedules the user task of the current tenant, replacing the one already scheduled under the same key.
     *
     * @param task the task to schedule
     */
    public void createOrUpdateActiveUserTask(TaskDTO task) {
        createOrUpdateActiveTask(task, userSchedulers);
    }

    private void createOrUpdateActiveSystemTask(TaskDTO task) {
        createOrUpdateActiveTask(task, systemSchedulers);
    }

    private void createOrUpdateActiveTask(TaskDTO task, Map<String, Map<String, ScheduledTask>> schedulers) {
        ScheduledTask scheduled = scheduleReplacing(task, schedulers);
        if (scheduled == null) {
            return;
        }
        if (scheduled.cancelledOld() != null) {
            log.info("cancel active task: {} for update. cancelled result: {}", getTaskKey(task),
                scheduled.cancelledOld());
        } else {
            log.info("create new task during update: {}", getTaskKey(task));
        }
    }

    /**
     * Schedules the task in the current tenant, cancelling the schedule it replaces. Logs nothing. Returns the new
     * schedule, or null when the task type cannot be scheduled.
     */
    private ScheduledTask scheduleReplacing(TaskDTO task, Map<String, Map<String, ScheduledTask>> schedulers) {

        //Synchronization is necessary because task can start earlier than its future is created in collection
        CountDownLatch latch = new CountDownLatch(BigInteger.ONE.intValue());
        String currentTenant = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder);
        task.setTenant(currentTenant);

        ScheduledTask scheduled = schedulers.computeIfAbsent(currentTenant, tenant -> new ConcurrentHashMap<>())
            .compute(getTaskKey(task), (taskKey, old) -> replace(task, old, latch));
        latch.countDown();
        return scheduled;
    }

    private ScheduledTask replace(TaskDTO task, ScheduledTask old, CountDownLatch latch) {
        Boolean cancelledOld = old == null ? null : cancelTask(old.future());
        ScheduledFuture<?> future = schedule(task, latch);
        return future == null ? null : new ScheduledTask(task, future, cancelledOld);
    }

    /**
     * Cancels and removes the task with the key from both the user and the system map of the current tenant.
     * Does nothing when no task has the key.
     *
     * @param taskKey the key of the task
     */
    public void deleteActiveTask(String taskKey) {
        deleteTaskFromTenant(getTenant(), taskKey);
    }

    public Set<String> getActiveSystemTaskKeys() {
        return Collections.unmodifiableSet(systemSchedulers.getOrDefault(getTenant(), Collections.emptyMap()).keySet());
    }

    /**
     * Gets the keys of the user tasks scheduled in the current tenant.
     *
     * @return the keys
     */
    public Set<String> getActiveUserTaskKeys() {
        return Collections.unmodifiableSet(userSchedulers.getOrDefault(getTenant(), Collections.emptyMap()).keySet());
    }

    /**
     * Removes a finished task, but only if the map still holds this very DTO: a firing thread must never
     * remove a newer schedule of the same key.
     */
    void deleteExpiredTask(TaskDTO task) {
        removeExpired(task, userSchedulers);
        removeExpired(task, systemSchedulers);
    }

    private void removeExpired(TaskDTO task, Map<String, Map<String, ScheduledTask>> schedulers) {
        String key = getTaskKey(task);
        Map<String, ScheduledTask> tenantMap = schedulers.get(task.getTenant());
        ScheduledTask entry = tenantMap == null ? null : tenantMap.get(key);
        if (entry != null && entry.task() != task) {
            return;
        }
        boolean removed = entry != null && tenantMap.remove(key, entry);
        boolean cancelled = removed && cancelTask(entry.future());
        log.info("task [{}] removed, cancel result: {}", key, cancelled);
    }

    void handleTask(TaskDTO task) {
        handler.handle(task);
        ownership.onSent(task);
    }

    private boolean isOwned(TaskDTO task) {
        return ownership.owns(task);
    }

    /**
     * Extension API for ownership implementations. Schedules the given user and system tasks in the current tenant, replacing the schedules of the same keys. A task
     * that cannot be scheduled is logged and counted, the others are still scheduled. Logs nothing for a task that
     * was scheduled; for bulk loads.
     */
    public ScheduleCounts scheduleQuietly(List<TaskDTO> userTasks, List<TaskDTO> systemTasks) {
        int invalid = scheduleCounted(userTasks, userSchedulers, "user")
            + scheduleCounted(systemTasks, systemSchedulers, "system");
        return new ScheduleCounts(userTasks.size() + systemTasks.size() - invalid, invalid);
    }

    /** Schedules the tasks, returns how many could not be scheduled. */
    private int scheduleCounted(List<TaskDTO> tasks, Map<String, Map<String, ScheduledTask>> schedulers,
                                String kind) {
        int invalid = 0;
        for (TaskDTO task : tasks) {
            if (!scheduleCounted(task, schedulers, kind)) {
                invalid++;
            }
        }
        return invalid;
    }

    private boolean scheduleCounted(TaskDTO task, Map<String, Map<String, ScheduledTask>> schedulers, String kind) {
        try {
            scheduleReplacing(task, schedulers);
            return true;
        } catch (Exception e) {
            log.error("{} task {} cannot be scheduled, skipped", kind, getTaskKey(task), e);
            return false;
        }
    }

    /**
     * Extension API for ownership implementations. Cancels and removes the user and system tasks of all tenants that
     * match. Returns how many were cancelled.
     */
    public int cancelIf(Predicate<TaskDTO> matches) {
        return Stream.of(userSchedulers, systemSchedulers)
            .flatMap(schedulers -> schedulers.values().stream())
            .mapToInt(tenantMap -> cancelMatching(tenantMap, matches))
            .sum();
    }

    private int cancelMatching(Map<String, ScheduledTask> tenantMap, Predicate<TaskDTO> matches) {
        List<Map.Entry<String, ScheduledTask>> selected = tenantMap.entrySet().stream()
            .filter(entry -> matches.test(entry.getValue().task()))
            .toList();
        int cancelled = 0;
        for (Map.Entry<String, ScheduledTask> entry : selected) {
            boolean removed = tenantMap.remove(entry.getKey(), entry.getValue());
            if (removed) {
                cancelTask(entry.getValue().future());
                cancelled++;
            }
        }
        return cancelled;
    }

    /**
     * Extension API for ownership implementations. The DTO a user task of the current tenant is scheduled from.
     * Empty when it is not scheduled.
     */
    public Optional<TaskDTO> findScheduledUserTask(String taskKey) {
        return Optional.ofNullable(userSchedulers.getOrDefault(getTenant(), Collections.emptyMap()).get(taskKey))
            .map(ScheduledTask::task);
    }

    private String getTenant() {
        return TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder);
    }

    private void deleteTaskFromTenant(String tenant, String taskKey) {
        deleteTaskFromTenant(tenant, taskKey, userSchedulers);
        deleteTaskFromTenant(tenant, taskKey, systemSchedulers);
    }

    private boolean deleteTaskFromTenant(String tenant, String taskKey,
                                         Map<String, Map<String, ScheduledTask>> schedulers) {
        boolean cancelled = Optional.ofNullable(schedulers.get(tenant))
            .map(tenantMap -> tenantMap.remove(taskKey))
            .map(st -> cancelTask(st.future()))
            .orElse(false);
        log.info("task [{}] removed, cancel result: {}", taskKey, cancelled);
        return cancelled;
    }

    /**
     * Set state for task.
     * Method init tenant context with value from input task because method called from another thread
     *
     * @param state new state of the task
     * @param task  the task changing
     */
    public void setState(StateKey state, TaskDTO task) {
        final Long taskId = task.getId();
        log.info("Marking task as finished id: {}, tenant: {}", task.getId(), task.getTenant());

        // targeted update: never save a whole entity from a stale read
        tenantContextHolder.getPrivilegedContext().execute(buildTenant(task.getTenant()), () -> {
            if (taskRepository.updateStateKey(taskId, state.toString()) == 0) {
                log.warn("Task not found for id {} while setting state {}", taskId, state);
            }
        });
    }

    private boolean cancelTask(ScheduledFuture<?> future) {
        boolean cancelled = false;
        if (future != null) {
            cancelled = future.cancel(false);
        }
        return cancelled;
    }

    private ScheduledFuture<?> schedule(TaskDTO task, CountDownLatch latch) {
        return schedule(new DefaultRunnableTask(task, this, afterRun, afterExpiration, latch, ownership));
    }

    private ScheduledFuture<?> schedule(RunnableTask expirable) {

        ScheduledFuture<?> future;
        TaskDTO task = expirable.getTask();

        switch (task.getScheduleType()) {
            case FIXED_DELAY:
                future = taskScheduler.scheduleWithFixedDelay(expirable, getStartDate(task), task.getDelay());
                break;
            case FIXED_RATE:
                future = taskScheduler.scheduleAtFixedRate(expirable, getStartDate(task), task.getDelay());
                break;
            case CRON:
                if (StringUtils.isNotBlank(task.getCronTriggerTimeZoneId())) {
                    ZoneId zoneId = ZoneId.of(task.getCronTriggerTimeZoneId());
                    TimeZone timeZone = TimeZone.getTimeZone(zoneId);
                    future = taskScheduler.schedule(expirable, new CronTrigger(task.getCronExpression(), timeZone));
                } else {
                    future = taskScheduler.schedule(expirable, new CronTrigger(task.getCronExpression()));
                }
                break;
            case ONE_TIME:
                future = scheduleOneTimeTask(expirable, task);
                break;
            default:
                log.warn("Task was not scheduled for unknown type: {}", task.getScheduleType());
                future = null;
        }

        return future;
    }

    private ScheduledFuture<?> scheduleOneTimeTask(RunnableTask expirable, TaskDTO task) {
        if (task.getEndDate() == null) {
            // only rows stored before end date was computed on save: fill the null, never overwrite a value
            task.setEndDate(TaskScheduleRules.oneTimeEndDate(task.getStartDate(), task.getTtl()));
            if (task.getId() != null) {
                taskRepository.backfillEndDate(task.getId(), task.getEndDate());
            }
        }
        return taskScheduler.schedule(expirable, task.getStartDate());
    }

    private static String getTaskKey(TaskDTO task) {
        return Optional.ofNullable(task.getId()).map(String::valueOf).orElse(task.getKey());
    }

    private static Date getStartDate(TaskDTO task) {
        return Optional.ofNullable(task.getStartDate()).map(d -> new Date(d.toEpochMilli())).orElse(new Date());
    }

}
