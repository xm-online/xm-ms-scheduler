package com.icthh.xm.ms.scheduler.service;

import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing Task.
 */
public interface TaskService {

    /**
     * Save a task.
     *
     * @param taskDTO the entity to save
     * @return the persisted entity
     */
    TaskDTO save(TaskDTO taskDTO);

    /**
     * Get all the tasks.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    Page<TaskDTO> findAll(Pageable pageable);

    /**
     * Get the "id" task.
     *
     * @param id the id of the entity
     * @return the entity
     */
    TaskDTO findOne(Long id);

    /**
     * Delete the "id" task.
     *
     * @param id the id of the entity
     */
    void delete(Long id);
}
