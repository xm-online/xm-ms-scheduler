package com.icthh.xm.ms.scheduler.manager;

import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;
import org.springframework.stereotype.Component;

/**
 *
 */
@Component
public interface ScheduledTaskHandler {

    void handle(TaskDTO task);

}
