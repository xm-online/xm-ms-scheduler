package com.icthh.xm.ms.scheduler.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.ms.scheduler.domain.ScheduledEvent;
import com.icthh.xm.ms.scheduler.nameresolver.ChannelNameResolver;
import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.messaging.support.MessageBuilder;

/**
 * Default implementation for scheduled task handler. Sends messages to spring cloud stream (kafka binding)
 *
 * <p>Receives {@link ChannelNameResolver} for tenant based resolver name creation
 *
 * <p>and {@link BinderAwareChannelResolver} for actual message sending into the channel.
 */
@RequiredArgsConstructor
@Slf4j
public class ScheduledTaskHandlerImpl implements ScheduledTaskHandler {

    private static final String DEFAULT_KEY = "value";
    private final BinderAwareChannelResolver channelResolver;
    private final ChannelNameResolver nameResolver;

    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public void handle(final TaskDTO task) {

        String channel = nameResolver.resolve(task);

        try {
            ScheduledEvent event = ScheduledEvent.builder()
                                                 .uuid(UUID.randomUUID().toString())
                                                 .handlingTime(Instant.now())
                                                 .id(task.getId())
                                                 .key(task.getKey())
                                                 .name(task.getName())
                                                 .typeKey(task.getTypeKey())
                                                 .stateKey(task.getStateKey())
                                                 .createdBy(task.getCreatedBy())
                                                 .startDate(task.getStartDate())
                                                 .endDate(task.getEndDate())
                                                 .channelType(task.getChannelType())
                                                 .data(Optional.ofNullable(task.getData())
                                                               .map(this::parseDataSilent).orElse(null))
                                                 .build();

            log.info("send into channel [{}], event: {}", channel, event);
            channelResolver.resolveDestination(channel).send(MessageBuilder.withPayload(event).build());

        } catch (Exception e) {
            log.error("unable to send task into channel [{}], task: {}, error: {}", channel, task, e.getMessage());
            throw new RuntimeException(e);
        }

    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    private Map<String, Object> parseDataSilent(String data) {
        if (data.startsWith("{")) {
            return mapper.readValue(data, Map.class);
        } else {
            Map<String, Object> map = new HashMap<>();
            map.put(DEFAULT_KEY, data);
            return map;
        }
    }

}
