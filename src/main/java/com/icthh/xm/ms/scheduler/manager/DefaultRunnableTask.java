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
            if (isExpiredBeforeExecution()) {
                deleteExpired();
            } else {
                log.info("execute scheduled task: {}", task);
                manager.handleTask(task);

                if (afterRun != null) {
                    afterRun.accept(task);
                }

                if (isExpiredAfterExecution()) {
                    // time to delete
                    deleteExpired();
                }
            }
        } finally {
            MdcUtils.clear();
        }

    }

    /**
     * Deletes the expired task.
     */
    private void deleteExpired() {
        manager.deleteExpiredTask(task);
        if (afterExpiry != null) {
            afterExpiry.accept(task);
        }
    }

    /**
     * Checks if task is expired according to its type.
     *
     * @return true if expired
     */
    private boolean isExpiredBeforeExecution() {
        switch (task.getScheduleType()) {
            case ONE_TIME:
                if (task.getEndDate() != null
                    && task.getEndDate().isBefore(Instant.now())) {
                    log.info("remove expired scheduled task {} due to expired ttl: {}",
                        task.getId(), task.getTtl());
                    return true;
                } else {
                    return false;
                }
            default:
                return false;
        }
    }

    /**
     * Checks if the task is expired according to its type.
     *
     * @return true if expired
     */
    private boolean isExpiredAfterExecution() {
        switch (task.getScheduleType()) {
            //if one time task was executed it is already expired
            case ONE_TIME:
                if (task.getStartDate() != null
                    && task.getStartDate().isBefore(Instant.now())) {
                    log.info("remove scheduled task {} due to finished execution", task.getId());
                    return true;
                } else {
                    return false;
                }
            case CRON:
                if (task.getEndDate() != null && task.getEndDate().isBefore(Instant.now())) {
                    log.info("remove expired scheduled task {} due to end date: {}", task.getId(), task.getEndDate());
                    return true;
                } else {
                    return false;
                }
            default:
                if (task.getEndDate() != null && task.getEndDate().isBefore(Instant.now()
                    .plusMillis(task.getDelay()))) {
                    log.info("remove expired scheduled task {} due to end date: {}", task.getId(), task.getEndDate());
                    return true;
                } else {
                    return false;
                }
        }
    }

    @Override
    public TaskDTO getTask() {
        return task;
    }
}
