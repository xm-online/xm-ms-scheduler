package com.icthh.xm.ms.scheduler.service.mapper;

import com.icthh.xm.ms.scheduler.domain.Task;
import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper for the entity Task and its DTO TaskDTO.
 */
@Mapper(componentModel = "spring")
public interface TaskMapper extends EntityMapper<TaskDTO, Task> {

    @Override
    @Mapping(target = "tenant", ignore = true)
    @Mapping(target = "cronTriggerTimeZoneId", ignore = true)
    TaskDTO toDto(Task entity);

    default Task fromId(Long id) {
        if (id == null) {
            return null;
        }
        Task task = new Task();
        task.setId(id);
        return task;
    }
}
