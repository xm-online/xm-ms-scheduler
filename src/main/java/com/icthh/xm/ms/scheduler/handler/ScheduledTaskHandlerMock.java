package com.icthh.xm.ms.scheduler.handler;

import com.icthh.xm.ms.scheduler.nameresolver.ChannelNameResolver;
import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;

import javax.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Mock class for scheduler task. Initialized in case if messaging system is disabled with property:
 *
 *<p>'application.stream-binding-enabled'
 *
 *<p>Should be used for test reasons.
 */
@Slf4j
@RequiredArgsConstructor
public class ScheduledTaskHandlerMock implements ScheduledTaskHandler {

    private final ChannelNameResolver nameResolver;

    @PostConstruct
    public void init() {
        log.warn("Scheduled task handler initialized with mock due to 'application.stream-binding-enabled'"
            + " property is false");
    }

    @Override
    public void handle(final TaskDTO task) {

        String channel = nameResolver.resolve(task);

        log.info("MOCK_HANDLER: skip sending to [{}], task.id = {}, task.key = {}", channel, task.getId(),
            task.getKey());

    }
}
