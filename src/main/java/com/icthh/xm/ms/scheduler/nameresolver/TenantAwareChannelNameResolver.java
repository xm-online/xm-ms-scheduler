package com.icthh.xm.ms.scheduler.nameresolver;

import com.icthh.xm.ms.scheduler.domain.enumeration.ChannelType;
import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;

import java.util.Optional;

/**
 * Tenant aware channel name resolver is used to dynamically calculate Spring cloud channel name (kafka topic) in
 * multi tenant context.
 */
public class TenantAwareChannelNameResolver implements ChannelNameResolver {

    private static final String PREFIX = "scheduler_";

    private static final String QUEUE = "queue";

    private static final String DELIMITER = "_";

    @Override
    public String resolve(final TaskDTO task) {
        return Optional.ofNullable(task.getTenant())
                       .map(tenant -> PREFIX + tenant + DELIMITER + getScheduleType(task.getChannelType()))
                       .orElseThrow(() -> new RuntimeException("Tenant can not be empty"));
    }

    private String getScheduleType(ChannelType type) {
        return Optional.ofNullable(type)
                       .map(Enum::toString)
                       .map(String::toLowerCase)
                       .orElse(QUEUE);
    }

}
