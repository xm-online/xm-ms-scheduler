package com.icthh.xm.ms.scheduler.manager;

import static com.icthh.xm.commons.tenant.TenantContextUtils.buildTenant;
import static java.time.temporal.ChronoUnit.HOURS;

import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.tenant.PrivilegedTenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.scheduler.domain.Task;
import com.icthh.xm.ms.scheduler.domain.enumeration.StateKey;
import com.icthh.xm.ms.scheduler.handler.ScheduledTaskHandler;
import com.icthh.xm.ms.scheduler.repository.TaskRepository;
import com.icthh.xm.ms.scheduler.service.SystemTaskService;
import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;

import java.math.BigInteger;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
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
import java.util.stream.Collectors;

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

    private Consumer<TaskDTO> afterRun;
    private Consumer<TaskDTO> afterExpiration;

    private Map<String, Map<String, ScheduledFuture>> systemSchedulers = new ConcurrentHashMap<>();
    private Map<String, Map<String, ScheduledFuture>> userSchedulers = new ConcurrentHashMap<>();

    /** The constructor. */
    public SchedulingManager(final TenantContextHolder tenantContextHolder,
                             final ThreadPoolTaskScheduler taskScheduler,
                             final SystemTaskService taskService,
                             final ScheduledTaskHandler handler,
                             final TenantListRepository tenantListRepository,
                             final TaskRepository taskRepository) {
        this.tenantContextHolder = tenantContextHolder;
        this.taskScheduler = taskScheduler;
        this.taskService = taskService;
        this.handler = handler;
        this.tenantListRepository = tenantListRepository;
        this.taskRepository = taskRepository;
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
        this(tenantContextHolder, taskScheduler, taskService, handler, tenantListRepository, taskRepository);
        this.afterRun = afterRun;
        this.afterExpiration = afterExpiration;
    }

    /**
     * Init.
     */
    public void init() {

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

            List<TaskDTO> newTasks = taskService.findSystemNotFinishedTasks();
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
                                     Map<String, Map<String, ScheduledFuture>> schedulers) {

        //Synchronization is necessary because task can start earlier than its future is created in collection
        CountDownLatch latch = new CountDownLatch(BigInteger.ONE.intValue());
        Map<String, ScheduledFuture> scheduledTasks = new ConcurrentHashMap<>();
        tasks.stream().peek(task -> task.setTenant(tenantName)).forEach(taskDTO -> {
            scheduledTasks.put(SchedulingManager.getTaskKey(taskDTO), this.schedule(taskDTO, latch));
        });
        schedulers.put(tenantName, scheduledTasks);
        latch.countDown();

        return tasks.size();
    }

    public void destroy() {
        log.info("destroy task scheduler...");
        long cnt = destroy(userSchedulers);
        log.info("cancelled user tasks count: {}", cnt);
        cnt = destroy(systemSchedulers);
        log.info("cancelled system tasks count: {}", cnt);
    }

    private long destroy(Map<String, Map<String, ScheduledFuture>> schedulers) {
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

    private void createOrUpdateActiveTask(TaskDTO task, Map<String, Map<String, ScheduledFuture>> schedulers) {

        //Synchronization is necessary because task can start earlier than its future is created in collection
        CountDownLatch latch = new CountDownLatch(BigInteger.ONE.intValue());
        String currentTenant = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder);
        schedulers.computeIfAbsent(currentTenant, tenant -> new ConcurrentHashMap<>());

        task.setTenant(currentTenant);

        schedulers.get(currentTenant)
            .compute(getTaskKey(task), (taskKey, oldFuture) -> rescheduleTask(task, oldFuture, latch));
        latch.countDown();
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

    void deleteExpiredTask(TaskDTO task) {
        deleteTaskFromTenant(task.getTenant(), getTaskKey(task));
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
                                         Map<String, Map<String, ScheduledFuture>> schedulers) {
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

        tenantContextHolder.getPrivilegedContext().execute(buildTenant(task.getTenant()), () -> {

            Task taskFromDb = taskRepository.findById(taskId).orElseThrow(()
                -> new BusinessException("Task not found for id " + taskId));

            taskFromDb.setStateKey(state.toString());
            taskRepository.save(taskFromDb);
        });
    }

    private ScheduledFuture rescheduleTask(TaskDTO task, ScheduledFuture oldFuture, CountDownLatch latch) {
        if (oldFuture != null) {
            boolean cancelled = cancelTask(oldFuture);
            log.info("cancel active task: {} for update. cancelled result: {}", getTaskKey(task), cancelled);
        } else {
            log.info("create new task during update: {}", getTaskKey(task));
        }
        return schedule(task, latch);
    }

    private boolean cancelTask(ScheduledFuture future) {
        boolean cancelled = false;
        if (future != null) {
            cancelled = future.cancel(false);
        }
        return cancelled;
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
        if (task.getTtl() != null) {
            task.setEndDate(task.getStartDate().plusSeconds(task.getTtl()));
        } else {
            task.setEndDate(task.getStartDate().plus(1, HOURS));
        }
        Task entity = taskRepository.findById(task.getId()).orElseThrow(() ->
            new BusinessException("Task is not found for id " + task.getId()));
        entity.setEndDate(task.getEndDate());
        taskRepository.save(entity);
        return taskScheduler.schedule(expirable, task.getStartDate());
    }

    private static String getTaskKey(TaskDTO task) {
        return Optional.ofNullable(task.getId()).map(String::valueOf).orElse(task.getKey());
    }

    private static Date getStartDate(TaskDTO task) {
        return Optional.ofNullable(task.getStartDate()).map(d -> new Date(d.toEpochMilli())).orElse(new Date());
    }

}
