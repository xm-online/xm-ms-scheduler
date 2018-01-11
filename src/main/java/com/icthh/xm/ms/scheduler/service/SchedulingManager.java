package com.icthh.xm.ms.scheduler.service;

import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 *
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SchedulingManager {

    final ThreadPoolTaskScheduler taskScheduler;
    final TaskServiceExt taskService;
    final Consumer<TaskDTO> afterRun;
    final Consumer<TaskDTO> afterExpiration;

    private Map<String, ScheduledFuture> activeSchedulers = new ConcurrentHashMap<>();

    public void init() {

        List<TaskDTO> tasks = taskService.findAllNotFinishedTasks();

        log.info("start init Scheduler component with [{}] active tasks...", tasks.size());

        tasks.forEach(taskDTO -> activeSchedulers.compute(getTaskKey(taskDTO), (k, v) -> {

            if (v == null || v.isCancelled()) {
                return schedule(taskDTO, () -> new DefaultExpirable(taskDTO, this, afterRun, afterExpiration));
            }

            return null;
        }));

        log.info("stop init Scheduler component with [{}] active tasks.", tasks.size());

    }

    public void destroy() {
        log.info("destroy task scheduler...");
        taskScheduler.shutdown();
    }

    public ScheduledFuture getActiveTask(String taskKey) {
        return activeSchedulers.get(taskKey);
    }

    public void updateActiveTask(TaskDTO task) {

        activeSchedulers.compute(getTaskKey(task), (k, future) -> {

            if (future != null) {
                boolean cancelled = future.cancel(false);
                log.info("cancel active scheduler: {} for update. cancelled result: {}", getTaskKey(task), cancelled);
            } else {
                log.info("create new scheduler: {}", getTaskKey(task));
            }
            return schedule(task, () -> new DefaultExpirable(task, this, afterRun, afterExpiration));

        });

    }

    public void deleteActiveTask(String taskKey) {

        ScheduledFuture future = activeSchedulers.remove(taskKey);
        boolean cancelled = false;
        if (future != null) {
            cancelled = future.cancel(false);
        }

        log.info("task {} removed, cancelled result: {}", taskKey, cancelled);

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

    private static String getTaskKey(TaskDTO task) {
        return Optional.ofNullable(task.getId()).map(String::valueOf).orElse(task.getKey());
    }

    private static Date getStartDate(TaskDTO task) {
        return Optional.ofNullable(task.getStartDate()).map(d -> new Date(d.toEpochMilli())).orElse(new Date());
    }

    private Runnable createSchedulerLogic(final TaskDTO task, SchedulingManager mng) {
        return () -> {
            log.info("execute scheduled task: {}", task);

            if (task.getEndDate() != null && task.getEndDate().isBefore(Instant.now().plusMillis(task.getDelay()))) {
                // tome to delete
                log.info("remove active scheduled task {} due to end date: {}", task.getId(), task.getEndDate());
                mng.deleteActiveTask(getTaskKey(task));
            }

        };
    }

    public interface Expirable extends Runnable {

        boolean isExpired();

    }

    @RequiredArgsConstructor
    @Slf4j
    public static class DefaultExpirable implements Expirable {

        final TaskDTO task;
        final SchedulingManager manager;
        final Consumer<TaskDTO> afterRun;
        final Consumer<TaskDTO> afterExpiry;

        @Override
        public void run() {
            log.info("execute scheduled task: {}", task);

            afterRun.accept(task);

            if (isExpired()) {
                // time to delete
                log.info("remove active scheduled task {} due to end date: {}", task.getId(), task.getEndDate());
                manager.deleteActiveTask(getTaskKey(task));
                afterExpiry.accept(task);
            }
        }

        @Override
        public boolean isExpired() {
            log.info("task.getEndDate() = {}, now = {}", task.getEndDate(), Instant.now());
            return task.getEndDate() != null && task.getEndDate().isBefore(Instant.now().plusMillis(task.getDelay()));
        }
    }

}
