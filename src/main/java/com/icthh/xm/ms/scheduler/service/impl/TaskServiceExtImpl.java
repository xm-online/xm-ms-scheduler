package com.icthh.xm.ms.scheduler.service.impl;

import static java.util.stream.Collectors.toList;

import com.icthh.xm.ms.scheduler.repository.TaskRepository;
import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;
import com.icthh.xm.ms.scheduler.service.mapper.TaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
@Service
@Transactional
@Slf4j
@Primary
public class TaskServiceExtImpl extends TaskServiceImpl {

    public TaskServiceExtImpl(final TaskRepository taskRepository,
                              final TaskMapper taskMapper) {
        super(taskRepository, taskMapper);
    }

    /**
     * Get all the tasks.
     *
     * @return the list of entities
     */
    @Transactional(readOnly = true)
    public List<TaskDTO> findAllNotFinishedTasks() {
        log.debug("Request to get all Tasks without paging");
        return taskRepository.findByEndDateGreaterThanEqual(Instant.now())
                             .stream().map(taskMapper::toDto).collect(toList());
    }

}
