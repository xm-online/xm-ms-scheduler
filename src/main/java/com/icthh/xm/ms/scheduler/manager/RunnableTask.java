package com.icthh.xm.ms.scheduler.manager;

import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;

/**
 * The runnable task interface.
 */ // TODO - to think if we really need this interface
public interface RunnableTask extends Runnable {

    TaskDTO getTask();

}
