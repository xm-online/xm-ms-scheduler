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

/**
 * Scheduling manager component is designed to handle all active scheduled tasks.
 */
@Slf4j
public class SchedulingManager {

    final TenantContextHolder tenantContextHolder;
    final ThreadPoolTaskScheduler taskScheduler;
    final SystemTaskService taskService;
    final ScheduledTaskHandler handler;
    final TenantListRepository tenantListRepository;

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
        //TODO - review logic
        String defaultTenantName = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder);

        int countOfTasks = 0;

        log.info("Before init scheduler tasks, defaultTenant = {}", defaultTenantName);

        for (String tenantName : tenantListRepository.getTenants()) {

            PrivilegedTenantContext ptc = tenantContextHolder.getPrivilegedContext();

            int processed = ptc.execute(TenantContextUtils.buildTenant(tenantName), () -> {
                log.info("Start initialization of tasks for tenant [{}]", tenantName);

                List<TaskDTO> tasks = taskService.findAllNotFinishedTasks();

                tasks.forEach(task -> activeSchedulers.compute(tenantName, (k, v) -> {
                    if (v == null) {
                        v = new HashMap();
                    }
                    task.setTenant(tenantName);
                    v.put(getTaskKey(task), schedule(new DefaultRunnableTask(task, this, afterRun, afterExpiration)));
                    return v;
                }));

                return tasks.size();

            });


            countOfTasks += processed;

            log.info("Count of inited tasks for tenant [{}]: {}", tenantName, processed);
        }

        log.info("SchedulingManager was started with [{}] active tasks. defaultTenant = {}", countOfTasks, defaultTenantName);
    }

    public void destroy() {
        log.info("destroy task scheduler...");
        long cnt = activeSchedulers.values().stream().flatMap(m -> m.values().stream()).map(this::cancelTask).count();
        log.info("tasks cancelled count: {}", cnt);
        activeSchedulers.clear();
    }

    public ScheduledFuture getActiveTask(String taskKey) {
        return Optional.ofNullable(activeSchedulers.get(TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder))).orElse(new HashMap<>()).get(taskKey);
    }

    public void updateActiveTask(TaskDTO task) {

        String currentTenant = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder);

        activeSchedulers.compute(currentTenant, (k, v) -> {
            if (v == null) {
                v = new HashMap();
            }
            if (v.get(getTaskKey(task)) != null) {
                boolean cancelled = cancelTask(v.get(getTaskKey(task)));
                log.info("cancel active scheduler: {} for update. cancelled result: {}", getTaskKey(task), cancelled);
            } else {
                log.info("create new scheduler: {}", getTaskKey(task));
            }
            task.setTenant(currentTenant);
            v.put(getTaskKey(task), schedule(new DefaultRunnableTask(task, this, afterRun, afterExpiration)));
            return v;
        });
    }

    public void deleteActiveTask(TaskDTO task) {
        String currentTenant = task.getTenant() != null ? task.getTenant() : TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder);
        deleteActiveTask(currentTenant, getTaskKey(task));
    }

    public void deleteActiveTask(String taskKey) {
        deleteActiveTask(TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder), taskKey);
    }

    private void deleteActiveTask(String tenant, String taskKey) {
        Map<String, ScheduledFuture> taskMap = Optional.ofNullable(activeSchedulers.get(tenant)).orElse(new HashMap<>());
        ScheduledFuture future = taskMap.remove(taskKey);
        boolean cancelled = cancelTask(future);
        log.info("task {} removed, cancelled result: {}", taskKey, cancelled);
    }

    void handleTask(TaskDTO task) {
        handler.handle(task);
    }

    private boolean cancelTask(ScheduledFuture future) {
        boolean cancelled = false;
        if (future != null) {
            cancelled = future.cancel(false);
        }
        return cancelled;
    }

    private ScheduledFuture schedule(RunnableTask expirable) {

        ScheduledFuture future;

        TaskDTO task = expirable.getTask();

        switch (task.getScheduleType()) {
            case FIXED_DELAY:
                future = taskScheduler.scheduleWithFixedDelay(expirable,
                                                              getStartDate(task),
                                                              task.getDelay());

                break;
            case FIXED_RATE:
                future = taskScheduler.scheduleAtFixedRate(expirable,
                                                           getStartDate(task),
                                                           task.getDelay());
                break;
            case CRON:
                future = taskScheduler.schedule(expirable,
                                                new CronTrigger(task.getCronExpression()));
                break;
            default:
                log.warn("Task was not scheduled for unknown type: {}", task.getScheduleType());
                future = null;
        }

        return future;
    }

    public static String getTaskKey(TaskDTO task) {
        return Optional.ofNullable(task.getId()).map(String::valueOf).orElse(task.getKey());
    }

    private static Date getStartDate(TaskDTO task) {
        return Optional.ofNullable(task.getStartDate()).map(d -> new Date(d.toEpochMilli())).orElse(new Date());
    }

}
