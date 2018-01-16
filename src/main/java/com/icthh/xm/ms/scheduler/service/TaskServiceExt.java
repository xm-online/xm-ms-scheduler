package com.icthh.xm.ms.scheduler.service;

import static java.util.stream.Collectors.toList;

import com.icthh.xm.ms.scheduler.domain.enumeration.ScheduleType;
import com.icthh.xm.ms.scheduler.repository.TaskRepository;
import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;
import com.icthh.xm.ms.scheduler.service.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 *
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class TaskServiceExt {

    final TaskRepository taskRepository;

    final TaskMapper taskMapper;

    /**
     * Get all the tasks.
     *
     * @return the list of entities
     */
    @Transactional(readOnly = true)
    public List<TaskDTO> findAllNotFinishedTasks() {
        log.debug("Request to get all Tasks without paging");

        List<TaskDTO> tasks = taskRepository.findByEndDateGreaterThanEqual(Instant.now())
                                            .stream().map(taskMapper::toDto).collect(toList());

        tasks.addAll(findAllNotFinishedTaskFromConfig());

        return tasks;
    }

    // TODO use RefreshableConfig instead of this mock
    List<TaskDTO> findAllNotFinishedTaskFromConfig() {
        return findAllTaskFromConfig();
    }

    // TODO use RefreshableConfig instead of this mock
    public List<TaskDTO> findAllTaskFromConfig() {

        TaskDTO task = new TaskDTO();
        task.setKey("systask1");
        task.setScheduleType(ScheduleType.FIXED_DELAY);
        task.setDelay(1000L);
        task.setTypeKey("SYSTEM.TASK1");
        task.setData("{\"key\": \"value\"}");

        return Arrays.asList(task);
    }

    public TaskDTO findOneTaskFromConfigByKey(final String key) {
        return findAllTaskFromConfig().stream().filter(dto -> key.equals(dto.getKey())).findFirst().orElse(null);
    }
}
