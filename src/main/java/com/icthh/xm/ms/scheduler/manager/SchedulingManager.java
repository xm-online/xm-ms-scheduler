package com.icthh.xm.ms.scheduler.manager;

import static com.icthh.xm.commons.tenant.TenantContextUtils.buildTenant;
import static com.icthh.xm.ms.scheduler.domain.enumeration.ScheduleType.CRON;
import static com.icthh.xm.ms.scheduler.domain.enumeration.ScheduleType.FIXED_DELAY;
import static com.icthh.xm.ms.scheduler.domain.enumeration.ScheduleType.FIXED_RATE;
import static com.icthh.xm.ms.scheduler.domain.enumeration.ScheduleType.ONE_TIME;
import static java.time.temporal.ChronoUnit.HOURS;

import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.tenant.PrivilegedTenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.scheduler.domain.enumeration.ScheduleType;
import com.icthh.xm.ms.scheduler.domain.enumeration.StateKey;
import com.icthh.xm.ms.scheduler.handler.ScheduledTaskHandler;
import com.icthh.xm.ms.scheduler.repository.TaskRepository;
import com.icthh.xm.ms.scheduler.service.SystemTaskService;
import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronExpression;
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
    private final TaskExecutionPolicy policy;

    private Consumer<TaskDTO> afterRun;
    private Consumer<TaskDTO> afterExpiration;

    private Map<String, Map<String, Scheduled>> systemSchedulers = new ConcurrentHashMap<>();
    private Map<String, Map<String, Scheduled>> userSchedulers = new ConcurrentHashMap<>();

    /** A task that the manager did not schedule, with the reason. */
    public record Rejected(TaskDTO task, String reason) {
    }

    private record Scheduled(TaskDTO task, ScheduledFuture<?> future) {
    }

    /** The constructor. */
    public SchedulingManager(final TenantContextHolder tenantContextHolder,
                             final ThreadPoolTaskScheduler taskScheduler,
                             final SystemTaskService taskService,
                             final ScheduledTaskHandler handler,
                             final TenantListRepository tenantListRepository,
                             final TaskRepository taskRepository,
                             final TaskExecutionPolicy policy) {
        this.tenantContextHolder = tenantContextHolder;
        this.taskScheduler = taskScheduler;
        this.taskService = taskService;
        this.handler = handler;
        this.tenantListRepository = tenantListRepository;
        this.taskRepository = taskRepository;
        this.policy = policy;
    }

    /** The constructor. */
    public SchedulingManager(final TenantContextHolder tenantContextHolder,
                             final ThreadPoolTaskScheduler taskScheduler,
                             final SystemTaskService taskService,
                             final ScheduledTaskHandler handler,
                             final Consumer<TaskDTO> afterRun,
                             final Consumer<TaskDTO> afterExpiration,
                             final TenantListRepository tenantListRepository,
                             final TaskRepository taskRepository,
                             final TaskExecutionPolicy policy) {
        this(tenantContextHolder, taskScheduler, taskService, handler, tenantListRepository, taskRepository, policy);
        this.afterRun = afterRun;
        this.afterExpiration = afterExpiration;
    }

    /**
     * Init.
     */
    public void init() {
        if (!policy.loadsOnStartup()) {
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

    public void mergeSystemTasksFromConfig(final String tenant) {

        log.info("Merge system tasks");

        tenantContextHolder.getPrivilegedContext().execute(buildTenant(tenant), () -> {

            List<TaskDTO> newTasks = taskService.findSystemNotFinishedTasks().stream()
                .filter(policy::accepts).collect(Collectors.toList());
            Map<String, TaskDTO> remainingTasks = newTasks.stream()
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

        Integer sysTasks = initInsideTenant(tenantName, taskService.findSystemNotFinishedTasks(), systemSchedulers);
        log.info("Finish tenant [{}] initialization with [{}] system active tasks", tenantName, sysTasks);

        Integer userTasks = initInsideTenant(tenantName, taskService.findUserNotFinishedTasks(), userSchedulers);
        log.info("Finish tenant [{}] initialization with [{}] user active tasks", tenantName, userTasks);

        return sysTasks + userTasks;
    }

    private Integer initInsideTenant(final String tenantName, List<TaskDTO> tasks,
                                     Map<String, Map<String, Scheduled>> schedulers) {
        List<Rejected> rejected = scheduleAll(tasks, schedulers, this::replaceTask);
        rejected.forEach(r -> log.error("Task {} of tenant {} not scheduled: {}",
            getTaskKey(r.task()), tenantName, r.reason()));
        return tasks.size() - rejected.size();
    }

    public void destroy() {
        log.info("destroy task scheduler...");
        long cnt = destroy(userSchedulers);
        log.info("cancelled user tasks count: {}", cnt);
        cnt = destroy(systemSchedulers);
        log.info("cancelled system tasks count: {}", cnt);
    }

    private long destroy(Map<String, Map<String, Scheduled>> schedulers) {
        long cnt = schedulers.values().stream().flatMap(m -> m.values().stream()).map(this::cancelTask).count();
        schedulers.clear();
        return cnt;
    }

    public void createOrUpdateActiveUserTask(TaskDTO task) {
        createOrUpdateActiveTask(task, userSchedulers);
    }

    private void createOrUpdateActiveSystemTask(TaskDTO task) {
        createOrUpdateActiveTask(task, systemSchedulers);
    }

    private void createOrUpdateActiveTask(TaskDTO task, Map<String, Map<String, Scheduled>> schedulers) {
        validateSchedule(task);
        scheduleAll(List.of(task), schedulers, this::rescheduleTask);
    }

    /**
     * Schedules the tasks in the current tenant; each replaces the task with the same key.
     * Tasks that fail {@link #validateSchedule} are returned. Logs nothing above DEBUG.
     */
    public List<Rejected> scheduleUserTasks(Collection<TaskDTO> tasks) {
        return scheduleAll(tasks, userSchedulers, this::replaceTask);
    }

    /** Same as {@link #scheduleUserTasks} for system tasks. */
    public List<Rejected> scheduleSystemTasks(Collection<TaskDTO> tasks) {
        return scheduleAll(tasks, systemSchedulers, this::replaceTask);
    }

    private List<Rejected> scheduleAll(Collection<TaskDTO> tasks, Map<String, Map<String, Scheduled>> schedulers,
                                       Replacer replacer) {
        String tenant = getTenant();
        Map<String, Scheduled> scheduled = schedulers.computeIfAbsent(tenant, t -> new ConcurrentHashMap<>());
        List<Rejected> rejected = new ArrayList<>();
        //Synchronization is necessary because task can start earlier than its future is created in collection
        CountDownLatch latch = new CountDownLatch(1);
        try {
            for (TaskDTO task : tasks) {
                if (!policy.accepts(task)) {
                    continue;
                }
                try {
                    validateSchedule(task);
                } catch (BusinessException e) {
                    rejected.add(new Rejected(task, e.getMessage()));
                    continue;
                }
                task.setTenant(tenant);
                scheduled.compute(getTaskKey(task), (key, old) -> replacer.replace(task, old, latch));
            }
        } finally {
            latch.countDown();
        }
        return rejected;
    }

    /**
     * Cancels every scheduled task, of all tenants, that matches. Returns how many were cancelled.
     */
    public int cancelTasks(Predicate<TaskDTO> filter) {
        return cancelMatching(userSchedulers, filter) + cancelMatching(systemSchedulers, filter);
    }

    private int cancelMatching(Map<String, Map<String, Scheduled>> schedulers, Predicate<TaskDTO> filter) {
        int cancelled = 0;
        for (Map<String, Scheduled> tenantTasks : schedulers.values()) {
            for (Map.Entry<String, Scheduled> entry : tenantTasks.entrySet()) {
                if (filter.test(entry.getValue().task()) && tenantTasks.remove(entry.getKey(), entry.getValue())) {
                    cancelTask(entry.getValue());
                    cancelled++;
                }
            }
        }
        return cancelled;
    }

    /** The scheduled user tasks of the current tenant by task key. */
    public Map<String, TaskDTO> getActiveUserTasks() {
        return scheduledTasks(userSchedulers);
    }

    /** The scheduled system tasks of the current tenant by task key. */
    public Map<String, TaskDTO> getActiveSystemTasks() {
        return scheduledTasks(systemSchedulers);
    }

    private Map<String, TaskDTO> scheduledTasks(Map<String, Map<String, Scheduled>> schedulers) {
        return schedulers.getOrDefault(getTenant(), Collections.emptyMap()).entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> e.getValue().task()));
    }

    /**
     * Checks the fields that scheduling the task needs.
     *
     * @throws BusinessException when the task cannot be scheduled
     */
    public static void validateSchedule(TaskDTO task) {
        ScheduleType type = task.getScheduleType();
        String problem = null;
        if (type == null) {
            problem = "scheduleType is required";
        } else if ((type == FIXED_DELAY || type == FIXED_RATE) && (task.getDelay() == null || task.getDelay() <= 0)) {
            problem = "delay must be positive";
        } else if (type == CRON && !CronExpression.isValidExpression(task.getCronExpression())) {
            problem = "cronExpression is not valid: " + task.getCronExpression();
        } else if (type == CRON && !isZoneIdOrBlank(task.getCronTriggerTimeZoneId())) {
            problem = "cronTriggerTimeZoneId is not valid: " + task.getCronTriggerTimeZoneId();
        } else if (type == ONE_TIME && task.getStartDate() == null) {
            problem = "startDate is required for ONE_TIME";
        }
        if (problem != null) {
            throw new BusinessException("error.task.schedule.invalid", problem);
        }
    }

    private static boolean isZoneIdOrBlank(String zoneId) {
        try {
            return StringUtils.isBlank(zoneId) || ZoneId.of(zoneId) != null;
        } catch (DateTimeException e) {
            return false;
        }
    }

    /** The end of a ONE_TIME task: start date plus ttl seconds, or one hour. */
    public static Instant oneTimeEndDate(TaskDTO task) {
        return task.getTtl() != null ? task.getStartDate().plusSeconds(task.getTtl())
                                     : task.getStartDate().plus(1, HOURS);
    }

    public void deleteActiveTask(String taskKey) {
        deleteTaskFromTenant(getTenant(), taskKey);
    }

    public Set<String> getActiveSystemTaskKeys() {
        return Collections.unmodifiableSet(systemSchedulers.getOrDefault(getTenant(), Collections.emptyMap()).keySet());
    }

    /**
     * Gets the user schedulers map.
     *
     * @return the user schedulers map
     */
    public Set<String> getActiveUserTaskKeys() {
        return Collections.unmodifiableSet(userSchedulers.getOrDefault(getTenant(), Collections.emptyMap()).keySet());
    }

    /** Removes the task only while the schedule still holds this very instance: a newer version stays. */
    void deleteExpiredTask(TaskDTO task) {
        removeOwnEntry(task, userSchedulers);
        removeOwnEntry(task, systemSchedulers);
    }

    private void removeOwnEntry(TaskDTO task, Map<String, Map<String, Scheduled>> schedulers) {
        String key = getTaskKey(task);
        Map<String, Scheduled> tenantMap = schedulers.getOrDefault(task.getTenant(), Map.of());
        Scheduled current = tenantMap.get(key);
        if (current != null && current.task() == task && tenantMap.remove(key, current)) {
            log.info("task [{}] removed, cancel result: {}", key, cancelTask(current));
        }
    }

    void execute(TaskDTO task, Runnable firing) {
        policy.execute(task, firing);
    }

    void handleTask(TaskDTO task) {
        handler.handle(task);
    }

    private String getTenant() {
        return TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder);
    }

    private void deleteTaskFromTenant(String tenant, String taskKey) {
        deleteTaskFromTenant(tenant, taskKey, userSchedulers);
        deleteTaskFromTenant(tenant, taskKey, systemSchedulers);
    }

    private boolean deleteTaskFromTenant(String tenant, String taskKey,
                                         Map<String, Map<String, Scheduled>> schedulers) {
        boolean cancelled = Optional.ofNullable(schedulers.get(tenant))
            .map(tenantMap -> tenantMap.remove(taskKey))
            .map(this::cancelTask)
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

        tenantContextHolder.getPrivilegedContext().execute(buildTenant(task.getTenant()),
            () -> taskRepository.updateStateKey(taskId, state.name(), task.getStartDate()));
    }

    @FunctionalInterface
    private interface Replacer {
        Scheduled replace(TaskDTO task, Scheduled old, CountDownLatch latch);
    }

    private Scheduled replaceTask(TaskDTO task, Scheduled old, CountDownLatch latch) {
        if (old != null) {
            cancelTask(old);
        }
        return new Scheduled(task, schedule(task, latch));
    }

    private Scheduled rescheduleTask(TaskDTO task, Scheduled old, CountDownLatch latch) {
        if (old != null) {
            boolean cancelled = cancelTask(old);
            log.info("cancel active task: {} for update. cancelled result: {}", getTaskKey(task), cancelled);
        } else {
            log.info("create new task during update: {}", getTaskKey(task));
        }
        return new Scheduled(task, schedule(task, latch));
    }

    private boolean cancelTask(Scheduled scheduled) {
        return scheduled.future() != null && scheduled.future().cancel(false);
    }

    private ScheduledFuture schedule(TaskDTO task, CountDownLatch latch) {
        return schedule(new DefaultRunnableTask(task, this, afterRun, afterExpiration, latch));
    }

    private ScheduledFuture schedule(RunnableTask expirable) {

        ScheduledFuture future;
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

    private ScheduledFuture scheduleOneTimeTask(RunnableTask expirable, TaskDTO task) {
        if (task.getEndDate() == null) {
            task.setEndDate(oneTimeEndDate(task));
            if (task.getId() != null) {
                taskRepository.backfillEndDate(task.getId(), task.getEndDate());
            }
        }
        return taskScheduler.schedule(expirable, task.getStartDate());
    }

    public static String getTaskKey(TaskDTO task) {
        return Optional.ofNullable(task.getId()).map(String::valueOf).orElse(task.getKey());
    }

    private static Date getStartDate(TaskDTO task) {
        return Optional.ofNullable(task.getStartDate()).map(d -> new Date(d.toEpochMilli())).orElse(new Date());
    }

}
