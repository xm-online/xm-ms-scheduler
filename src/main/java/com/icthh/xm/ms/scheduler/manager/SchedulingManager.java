package com.icthh.xm.ms.scheduler.manager;

import com.icthh.xm.ms.scheduler.service.TaskServiceExt;
import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Scheduling manager component is designed to handle all active schedulers.
 */
@Component
@Slf4j
public class SchedulingManager {

    final ThreadPoolTaskScheduler taskScheduler;
    final TaskServiceExt taskService;
    Consumer<TaskDTO> afterRun;
    Consumer<TaskDTO> afterExpiration;

    public SchedulingManager(final ThreadPoolTaskScheduler taskScheduler,
                             final TaskServiceExt taskService) {
        this.taskScheduler = taskScheduler;
        this.taskService = taskService;
    }

    public SchedulingManager(final ThreadPoolTaskScheduler taskScheduler,
                             final TaskServiceExt taskService,
                             final Consumer<TaskDTO> afterRun,
                             final Consumer<TaskDTO> afterExpiration) {
        this(taskScheduler, taskService);
        this.afterRun = afterRun;
        this.afterExpiration = afterExpiration;
    }

    // TODO - make multi-tenant
    private Map<String, ScheduledFuture> activeSchedulers = new ConcurrentHashMap<>();

    public void init() {

        List<TaskDTO> tasks = taskService.findAllNotFinishedTasks();

        log.info("start init Scheduler component with [{}] active tasks...", tasks.size());

        tasks.forEach(taskDTO -> activeSchedulers.compute(getTaskKey(taskDTO), (k, v) -> {

            if (v == null || v.isCancelled()) {
                // TODO - FIXME - how to inject DefaultExpirable properly with spring?
                return schedule(taskDTO, () -> new DefaultExpirable(taskDTO, this, afterRun, afterExpiration));
            }

            return null;
        }));

        log.info("stop init Scheduler component with [{}] active tasks.", tasks.size());

    }

    public void destroy() {
        log.info("destroy task scheduler...");
        long cnt = activeSchedulers.values().stream().map(this::cancelTask).count();
        log.info("tasks cancelled count: {}", cnt);
        activeSchedulers.clear();
    }

    public ScheduledFuture getActiveTask(String taskKey) {
        return activeSchedulers.get(taskKey);
    }

    public void updateActiveTask(TaskDTO task) {

        activeSchedulers.compute(getTaskKey(task), (k, future) -> {

            if (future != null) {
                boolean cancelled = cancelTask(future);
                log.info("cancel active scheduler: {} for update. cancelled result: {}", getTaskKey(task), cancelled);
            } else {
                log.info("create new scheduler: {}", getTaskKey(task));
            }

            // TODO - FIXME - how to inject DefaultExpirable properly with spring?
            return schedule(task, () -> new DefaultExpirable(task, this, afterRun, afterExpiration));

        });

    }

    public void deleteActiveTask(String taskKey) {

        ScheduledFuture future = activeSchedulers.remove(taskKey);
        boolean cancelled = cancelTask(future);
        log.info("task {} removed, cancelled result: {}", taskKey, cancelled);

    }

    private boolean cancelTask(ScheduledFuture future) {
        boolean cancelled = false;
        if (future != null) {
            cancelled = future.cancel(false);
        }
        return cancelled;
    }

    private ScheduledFuture schedule(TaskDTO task, Supplier<Expirable> supplier) {

        ScheduledFuture future;

        switch (task.getScheduleType()) {
            case FIXED_DELAY:
                future = taskScheduler.scheduleWithFixedDelay(supplier.get(),
                                                              getStartDate(task),
                                                              task.getDelay());

                break;
            case FIXED_RATE:
                future = taskScheduler.scheduleAtFixedRate(supplier.get(),
                                                           getStartDate(task),
                                                           task.getDelay());
                break;
            case CRON:
                future = taskScheduler.schedule(supplier.get(),
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
