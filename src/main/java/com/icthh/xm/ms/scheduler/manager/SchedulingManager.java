package com.icthh.xm.ms.scheduler.manager;

import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.tenant.PrivilegedTenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.scheduler.handler.ScheduledTaskHandler;
import com.icthh.xm.ms.scheduler.service.SystemTaskService;
import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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

    private Consumer<TaskDTO> afterRun;
    private Consumer<TaskDTO> afterExpiration;

    private Map<String, Map<String, ScheduledFuture>> activeSchedulers = new ConcurrentHashMap<>();

    public SchedulingManager(final TenantContextHolder tenantContextHolder,
                             final ThreadPoolTaskScheduler taskScheduler,
                             final SystemTaskService taskService,
                             final ScheduledTaskHandler handler,
                             final TenantListRepository tenantListRepository) {
        this.tenantContextHolder = tenantContextHolder;
        this.taskScheduler = taskScheduler;
        this.taskService = taskService;
        this.handler = handler;
        this.tenantListRepository = tenantListRepository;
    }

    public SchedulingManager(final TenantContextHolder tenantContextHolder,
                      final ThreadPoolTaskScheduler taskScheduler,
                      final SystemTaskService taskService,
                      final ScheduledTaskHandler handler,
                      final Consumer<TaskDTO> afterRun,
                      final Consumer<TaskDTO> afterExpiration,
                      final TenantListRepository tenantListRepository) {
        this(tenantContextHolder, taskScheduler, taskService, handler, tenantListRepository);
        this.afterRun = afterRun;
        this.afterExpiration = afterExpiration;
    }

    public void init() {

        int taskCount = 0;

        log.info("Start init scheduled tasks");

        for (String tenantName : tenantListRepository.getTenants()) {
            PrivilegedTenantContext ptc = tenantContextHolder.getPrivilegedContext();
            taskCount += ptc.execute(TenantContextUtils.buildTenant(tenantName), () -> initInsideTenant(tenantName));
        }

        log.info("Finish Scheduler initialization with [{}] tenants and [{}] active tasks",
                 tenantListRepository.getTenants().size(), taskCount);
    }

    private Integer initInsideTenant(final String tenantName) {
        log.info("Start initialization tasks on behalf of tenant [{}]", tenantName);

        List<TaskDTO> tasks = taskService.findAllNotFinishedTasks();

        Map<String, ScheduledFuture> scheduledTasks = tasks
            .stream()
            .peek(task -> task.setTenant(tenantName))
            .collect(Collectors.toMap(SchedulingManager::getTaskKey, this::schedule));

        activeSchedulers.put(tenantName, scheduledTasks);

        log.info("Finish tenant [{}] initialization with [{}] active tasks", tenantName, tasks.size());

        return tasks.size();
    }

    public void destroy() {
        log.info("destroy task scheduler...");
        long cnt = activeSchedulers.values().stream().flatMap(m -> m.values().stream()).map(this::cancelTask).count();
        log.info("cancelled tasks count: {}", cnt);
        activeSchedulers.clear();
    }

    public void createOrUpdateActiveTask(TaskDTO task) {

        String currentTenant = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder);
        activeSchedulers.computeIfAbsent(currentTenant, tenant -> new HashMap<>());

        task.setTenant(currentTenant);

        activeSchedulers.get(currentTenant)
                        .compute(getTaskKey(task), (taskKey, oldFuture) -> rescheduleTask(task, oldFuture));
    }



    public void deleteActiveTask(String taskKey) {
        deleteTaskFromTenant(getTenant(), taskKey);
    }

    public Set<String> getActiveTaskKeys() {
        return Collections.unmodifiableSet(activeSchedulers.getOrDefault(getTenant(), Collections.emptyMap()).keySet());
    }


    void deleteExpiredTask(TaskDTO task) {
        deleteTaskFromTenant(task.getTenant(), getTaskKey(task));
    }

    void handleTask(TaskDTO task) {
        handler.handle(task);
    }

    private String getTenant(){
        return TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder);
    }


    private void deleteTaskFromTenant(String tenant, String taskKey) {
        boolean cancelled = Optional.ofNullable(activeSchedulers.get(tenant))
                                    .map(tenantMap -> tenantMap.remove(taskKey))
                                    .map(this::cancelTask)
                                    .orElse(false);
        log.info("task [{}] removed, cancel result: {}", taskKey, cancelled);
    }

    private ScheduledFuture rescheduleTask(TaskDTO task, ScheduledFuture oldFuture) {
        if (oldFuture != null) {
            boolean cancelled = cancelTask(oldFuture);
            log.info("cancel active task: {} for update. cancelled result: {}", getTaskKey(task), cancelled);
        } else {
            log.info("create new task during update: {}", getTaskKey(task));
        }
        return schedule(task);
    }

    private boolean cancelTask(ScheduledFuture future) {
        boolean cancelled = false;
        if (future != null) {
            cancelled = future.cancel(false);
        }
        return cancelled;
    }

    private ScheduledFuture schedule(TaskDTO task) {
        return schedule(new DefaultRunnableTask(task, this, afterRun, afterExpiration));
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
                future = taskScheduler.schedule(expirable, new CronTrigger(task.getCronExpression()));
                break;
            default:
                log.warn("Task was not scheduled for unknown type: {}", task.getScheduleType());
                future = null;
        }

        return future;
    }

    private static String getTaskKey(TaskDTO task) {
        return Optional.ofNullable(task.getId()).map(String::valueOf).orElse(task.getKey());
    }

    private static Date getStartDate(TaskDTO task) {
        return Optional.ofNullable(task.getStartDate()).map(d -> new Date(d.toEpochMilli())).orElse(new Date());
    }

}
