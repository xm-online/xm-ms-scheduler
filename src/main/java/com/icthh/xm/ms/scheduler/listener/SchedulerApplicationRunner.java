package com.icthh.xm.ms.scheduler.listener;

import com.icthh.xm.ms.scheduler.manager.SchedulingManager;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SchedulerApplicationRunner implements ApplicationRunner {

    private final SchedulingManager schedulingManager;

    @Override
    public void run(ApplicationArguments applicationArguments) {
        schedulingManager.init();
    }
}
