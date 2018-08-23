package com.icthh.xm.ms.scheduler.listener;

import com.icthh.xm.ms.scheduler.manager.SchedulingManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class SchedulerApplicationRunner implements ApplicationRunner {

    @Autowired
    private SchedulingManager schedulingManager;

    @Override
    public void run(ApplicationArguments applicationArguments) throws Exception {
        schedulingManager.init();
    }
}
