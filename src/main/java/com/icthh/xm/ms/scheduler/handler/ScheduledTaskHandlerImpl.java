package com.icthh.xm.ms.scheduler.handler;

import com.icthh.xm.ms.scheduler.nameresolver.ChannelNameResolver;
import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 *
 */
@RequiredArgsConstructor
public class ScheduledTaskHandlerImpl implements ScheduledTaskHandler {

    final BinderAwareChannelResolver channelResolver;
    final ChannelNameResolver nameResolver;

    @Override
    public void handle(final TaskDTO task) {
        channelResolver.resolveDestination(nameResolver.resolve(task)).send(MessageBuilder.withPayload(task).build());
    }
}
