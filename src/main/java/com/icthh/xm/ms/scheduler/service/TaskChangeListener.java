package com.icthh.xm.ms.scheduler.service;

import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;

/**
 * Told after a task is saved or deleted through the task service.
 * The default bean schedules or cancels the task in this instance.
 */
public interface TaskChangeListener {

    /**
     * Called inside the save transaction with the saved task (id set).
     */
    void onSave(TaskDTO task);

    /**
     * Called inside the delete transaction, before the row is deleted.
     */
    void onDelete(Long taskId);
}
