package com.icthh.xm.ms.scheduler.manager;

import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.function.Consumer;

/**
 *
 */
@RequiredArgsConstructor
@Slf4j
public class DefaultExpirable implements Expirable {

    final TaskDTO task;
    final SchedulingManager manager;
    final Consumer<TaskDTO> afterRun;
    final Consumer<TaskDTO> afterExpiry;

    @Override
    public void run() {
        log.info("execute scheduled task: {}", task);

        // TODO - implement sending to event queue here (try to use Spring Cloud Streams)

        if (afterRun != null) {
            afterRun.accept(task);
        }

        if (isExpired()) {
            // time to delete
            log.info("remove active scheduled task {} due to end date: {}", task.getId(), task.getEndDate());
            manager.deleteActiveTask(SchedulingManager.getTaskKey(task));
            if (afterExpiry != null) {
                afterExpiry.accept(task);
            }
        }
    }

    @Override
    public boolean isExpired() {
        log.info("task.getEndDate() = {}, now = {}", task.getEndDate(), Instant.now());
        return task.getEndDate() != null && task.getEndDate().isBefore(Instant.now().plusMillis(task.getDelay()));
    }
}
