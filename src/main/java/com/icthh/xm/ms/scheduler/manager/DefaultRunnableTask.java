package com.icthh.xm.ms.scheduler.manager;

import static com.icthh.xm.ms.scheduler.domain.enumeration.ScheduleType.ONE_TIME;

import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.ms.scheduler.domain.enumeration.StateKey;
import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of scheduler task.
 **/
@RequiredArgsConstructor
@Slf4j
public class DefaultRunnableTask implements RunnableTask {

    final TaskDTO task;
    final SchedulingManager manager;
    final Consumer<TaskDTO> afterRun;
    final Consumer<TaskDTO> afterExpiry;
    final CountDownLatch latch;
    final TaskOwnership ownership;

    @Override
    @SneakyThrows
    public void run() {

        //wait until task is created in collection
        latch.await();

        MdcUtils.putRid(MdcUtils.generateRid() + "::" + task.getTenant());

        try {
            // a frozen and resumed instance must not send or change anything for tasks it no longer owns
            if (ownership.beginFiring(task)) {
                try {
                    execute();
                } finally {
                    ownership.endFiring(task);
                }
            } else {
                log.debug("skip task {}: not owned now", task.getId() != null ? task.getId() : task.getKey());
            }
        } finally {
            MdcUtils.clear();
        }

    }

    private void execute() {
        if (isExpiredBeforeExecution()) {
            deleteExpired();
            return;
        }

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

    /**
     * Deletes the expired task.
     */
    private void deleteExpired() {
        try {
            if (ONE_TIME.equals(task.getScheduleType())) {
                // DONE is committed before the task leaves the map, otherwise a reload in between fires it again
                manager.setState(StateKey.DONE, task);
            }
        } finally {
            manager.deleteExpiredTask(task);
            if (afterExpiry != null) {
                afterExpiry.accept(task);
            }
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
