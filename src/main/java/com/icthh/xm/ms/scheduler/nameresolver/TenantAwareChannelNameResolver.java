package com.icthh.xm.ms.scheduler.nameresolver;

import com.icthh.xm.ms.scheduler.domain.enumeration.ChannelType;
import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;

import java.util.Optional;

/**
 *
 */
public class TenantAwareChannelNameResolver implements ChannelNameResolver {

    public static final String PREFIX = "scheduler_";

    public static final String QUEUE = "queue";

    private static final String DELIMITER = "_";

    @Override
    public String resolve(final TaskDTO task) {
        return PREFIX + task.getTenant() + DELIMITER + getScheduleType(task.getChannelType());
    }

    private String getScheduleType(ChannelType type) {
        return Optional.ofNullable(type).map(Enum::toString).map(String::toLowerCase).orElse(QUEUE);
    }

}
