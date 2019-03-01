package com.icthh.xm.ms.scheduler.handler;

import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;
import org.springframework.stereotype.Component;

/**
 * Scheduled Task handler executed when task schedule time becomes.
 */
@Component
public interface ScheduledTaskHandler {

    void handle(TaskDTO task);

}
