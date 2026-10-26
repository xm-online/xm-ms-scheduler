package com.icthh.xm.ms.scheduler.service;

import com.icthh.xm.ms.scheduler.manager.SchedulingManager;
import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;

import lombok.RequiredArgsConstructor;

/**
 * Single instance mode: changes go straight to the scheduling manager.
 */
@RequiredArgsConstructor
public class LocalTaskChangeNotifier implements TaskChangeNotifier {

    private final SchedulingManager schedulingManager;

    @Override
    public void taskSaved(TaskDTO task) {
        schedulingManager.createOrUpdateActiveUserTask(task);
    }

    @Override
    public void taskDeleted(Long id) {
        schedulingManager.deleteActiveTask(String.valueOf(id));
    }

    @Override
    public void systemTasksChanged(String tenant) {
        schedulingManager.mergeSystemTasksFromConfig(tenant);
    }
}
