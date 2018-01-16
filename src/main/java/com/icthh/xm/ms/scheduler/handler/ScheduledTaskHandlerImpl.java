package com.icthh.xm.ms.scheduler.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.ms.scheduler.domain.ScheduledEvent;
import com.icthh.xm.ms.scheduler.nameresolver.ChannelNameResolver;
import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.messaging.support.MessageBuilder;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 *
 */
@RequiredArgsConstructor
@Slf4j
public class ScheduledTaskHandlerImpl implements ScheduledTaskHandler {

    public static final String DEFAULT_KEY = "value";
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
            log.error("unable to send task into channel [{}], task: {}", channel, task);
            throw new RuntimeException(e);
        }

    }

    private Map<String, Object> parseDataSilent(String data) {
        try {
            if (data.startsWith("{") || data.startsWith("[")) {
                return mapper.readValue(data, Map.class);
            } else {

                Map<String, Object> map = new HashMap<>();
                map.put(DEFAULT_KEY, data);
                return map;
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
