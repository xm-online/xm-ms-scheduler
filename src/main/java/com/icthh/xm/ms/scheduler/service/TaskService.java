package com.icthh.xm.ms.scheduler.service;

import com.icthh.xm.ms.scheduler.domain.Task;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.ms.scheduler.manager.SchedulingManager;
import com.icthh.xm.ms.scheduler.repository.TaskRepository;
import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;
import com.icthh.xm.ms.scheduler.service.mapper.TaskMapper;
import com.icthh.xm.ms.scheduler.domain.enumeration.ScheduleType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * Service Implementation for managing Task.
 */
@Service
@Transactional
public class TaskService {

    private final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;

    private final TaskMapper taskMapper;

    private final TaskChangeListener changeListener;

    public TaskService(TaskRepository taskRepository, TaskMapper taskMapper, TaskChangeListener changeListener) {
        this.taskRepository = taskRepository;
        this.taskMapper = taskMapper;
        this.changeListener = changeListener;
    }

    /**
     * Save a task.
     *
     * @param taskDtO the entity to save
     * @return the persisted entity
     */
    public TaskDTO save(TaskDTO taskDtO) {
        log.debug("Request to save Task : {}", taskDtO);
        if (StringUtils.isNotBlank(taskDtO.getCronTriggerTimeZoneId())) {
            throw new BusinessException("error.task.cron-trigger-time-zone-id.not-supported",
                "cronTriggerTimeZoneId is not stored and not supported");
        }
        SchedulingManager.validateSchedule(taskDtO);
        if (taskDtO.getScheduleType() == ScheduleType.ONE_TIME) {
            taskDtO.setEndDate(SchedulingManager.oneTimeEndDate(taskDtO));
        }
        Task task = taskMapper.toEntity(taskDtO);
        task = taskRepository.saveAndFlush(task);
        TaskDTO dto = taskMapper.toDto(task);
        changeListener.onSave(dto);
        return dto;
    }

    /**
     * Get all the tasks.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    @Transactional(readOnly = true)
    public Page<TaskDTO> findAll(Pageable pageable) {
        log.debug("Request to get all Tasks");
        return taskRepository.findAll(pageable)
            .map(taskMapper::toDto);
    }

    /**
     * Get one task by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    @Transactional(readOnly = true)
    public TaskDTO findOne(Long id) {
        log.debug("Request to get Task : {}", id);
        Task task = taskRepository.findById(id).orElse(null);
        return taskMapper.toDto(task);
    }

    /**
     * Delete the task by id.
     *
     * @param id the id of the entity
     */
    public void delete(Long id) {
        log.debug("Request to delete Task : {}", id);
        changeListener.onDelete(id);
        taskRepository.deleteById(id);
    }
}
