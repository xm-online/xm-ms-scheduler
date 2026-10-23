package com.icthh.xm.ms.scheduler.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.icthh.xm.ms.scheduler.AbstractSpringBootTest;
import com.icthh.xm.ms.scheduler.listener.SchedulerApplicationRunner;
import com.icthh.xm.ms.scheduler.manager.LocalOwnership;
import com.icthh.xm.ms.scheduler.manager.TaskOwnership;
import com.icthh.xm.ms.scheduler.service.LocalTaskChangeNotifier;
import com.icthh.xm.ms.scheduler.service.TaskChangeNotifier;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/** Default (single instance) mode: the OSS beans are used and the runner loads everything. */
public class LocalModeUnitTest extends AbstractSpringBootTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private TaskOwnership ownership;

    @Autowired
    private TaskChangeNotifier notifier;

    @Test
    public void localSeamsAreUsed() {
        assertThat(ownership).isInstanceOf(LocalOwnership.class);
        assertThat(notifier).isInstanceOf(LocalTaskChangeNotifier.class);
        assertThat(context.getBeanNamesForType(SchedulerApplicationRunner.class)).hasSize(1);
        assertThat(ownership.owns(new com.icthh.xm.ms.scheduler.service.dto.TaskDTO())).isTrue();
        assertThat(ownership.loadsAllOnStartup()).isTrue();
        assertThat(ownership.beginFiring(null)).isTrue();
    }
}
