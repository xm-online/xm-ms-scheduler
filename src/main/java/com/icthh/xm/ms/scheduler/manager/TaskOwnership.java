package com.icthh.xm.ms.scheduler.manager;

import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;

/**
 * Decides which tasks this instance runs. This is the contract between the scheduler and an ownership
 * implementation; the default, {@link LocalOwnership}, runs all tasks. An implementation is registered as the bean
 * {@code taskOwnership}.
 */
public interface TaskOwnership {

    /** True when this instance should schedule the task. */
    boolean owns(TaskDTO task);

    /** True when the startup runner loads every active task; false when the implementation loads tasks later. */
    boolean loadsAllOnStartup();

    /**
     * Called before a firing. True means the task may be sent now and is counted in flight: the caller must then call
     * {@link #endFiring}. False means the firing is skipped and nothing stays counted.
     */
    boolean beginFiring(TaskDTO task);

    /** Ends a firing that {@link #beginFiring} admitted. */
    void endFiring(TaskDTO task);

    /** Called after a task was sent. */
    default void onSent(TaskDTO task) {
    }
}
