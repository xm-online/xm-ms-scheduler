package com.icthh.xm.ms.scheduler.manager;

import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;

/**
 * Single instance mode: runs all tasks.
 */
public class LocalOwnership implements TaskOwnership {

    @Override
    public boolean owns(TaskDTO task) {
        return true;
    }

    @Override
    public boolean loadsAllOnStartup() {
        return true;
    }

    @Override
    public boolean beginFiring(TaskDTO task) {
        return true;
    }

    @Override
    public void endFiring(TaskDTO task) {
        // nothing is counted in single instance mode
    }
}
