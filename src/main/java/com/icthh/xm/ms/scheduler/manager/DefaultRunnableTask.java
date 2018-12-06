package com.icthh.xm.ms.scheduler.manager;

import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;

import java.time.Instant;
import java.util.function.Consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

        MdcUtils.putRid(MdcUtils.generateRid() + "::" + task.getTenant());

        try {
            log.info("execute scheduled task: {}", task);

            manager.handleTask(task);

            if (afterRun != null) {
                afterRun.accept(task);
            }

            if (isExpired()) {
                // time to delete
                log.info("remove expired scheduled task {} due to end date: {}", task.getId(), task.getEndDate());
                manager.deleteExpiredTask(task);
                if (afterExpiry != null) {
                    afterExpiry.accept(task);
                }
            }
        } finally {
            MdcUtils.clear();
        }

    }

    private boolean isExpired() {
        switch (task.getScheduleType()) {
            case CRON:
                return task.getEndDate() != null && task.getEndDate().isBefore(Instant.now());
            default:
                return task.getEndDate() != null && task.getEndDate().isBefore(Instant.now()
                    .plusMillis(task.getDelay()));
        }
    }

    @Override
    public TaskDTO getTask() {
        return task;
    }
}
