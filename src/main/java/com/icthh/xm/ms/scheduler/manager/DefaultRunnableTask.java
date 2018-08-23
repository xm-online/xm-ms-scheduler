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
public class DefaultRunnableTask implements RunnableTask {

    final TaskDTO task;
    final SchedulingManager manager;
    final Consumer<TaskDTO> afterRun;
    final Consumer<TaskDTO> afterExpiry;

    @Override
    public void run() {
        log.info("execute scheduled task: {}", task);

        manager.handleTask(task);

        if (afterRun != null) {
            afterRun.accept(task);
        }

        if (isExpired()) {
            // time to delete
            log.info("remove active scheduled task {} due to end date: {}", task.getId(), task.getEndDate());
            manager.deleteActiveTask(task);
            if (afterExpiry != null) {
                afterExpiry.accept(task);
            }
        }
    }

    private boolean isExpired() {
        return task.getEndDate() != null && task.getEndDate().isBefore(Instant.now().plusMillis(task.getDelay()));
    }

    @Override
    public TaskDTO getTask() {
        return task;
    }
}
