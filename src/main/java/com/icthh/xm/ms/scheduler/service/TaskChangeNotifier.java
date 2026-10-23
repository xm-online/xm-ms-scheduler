package com.icthh.xm.ms.scheduler.service;

import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;

/**
 * Reports task changes made through the service layer to whoever schedules them.
 */
public interface TaskChangeNotifier {

    void taskSaved(TaskDTO task);

    void taskDeleted(Long id);

    void systemTasksChanged(String tenant);
}
