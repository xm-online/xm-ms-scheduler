package com.icthh.xm.ms.scheduler.manager;

import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;

/**
 * Decides which tasks this instance schedules and runs each firing.
 * The default schedules and runs everything.
 */
public interface TaskExecutionPolicy {

    /**
     * Whether {@link SchedulingManager#init()} loads the stored tasks at startup.
     */
    default boolean loadsOnStartup() {
        return true;
    }

    /**
     * Whether this instance schedules the task. A task that is not accepted is ignored.
     */
    default boolean accepts(TaskDTO task) {
        return true;
    }

    /**
     * Runs one firing, including the state update that follows the send. May skip it.
     * Must not throw when it skips.
     */
    default void execute(TaskDTO task, Runnable firing) {
        firing.run();
    }
}
