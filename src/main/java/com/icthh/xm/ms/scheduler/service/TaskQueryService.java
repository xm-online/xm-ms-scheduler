package com.icthh.xm.ms.scheduler.service;

import com.icthh.xm.commons.permission.annotation.FindWithPermission;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.commons.permission.repository.CriteriaPermittedRepository;
import com.icthh.xm.ms.scheduler.domain.Task;
import com.icthh.xm.ms.scheduler.repository.TaskRepository;
import com.icthh.xm.ms.scheduler.service.dto.TaskCriteria;
import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;
import com.icthh.xm.ms.scheduler.service.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

import java.util.List;

/**
 * Service for executing complex queries for Task entities in the database.
 * The main input is a {@link TaskCriteria} which get's converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link List} of {@link TaskDTO} or a {@link Page} of {@link TaskDTO} which fulfills the criteria.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TaskQueryService extends QueryService<Task> {

    private final CriteriaPermittedRepository permittedRepository;

    private final TaskRepository taskRepository;

    private final TaskMapper taskMapper;

    /**
     * Return a {@link List} of {@link TaskDTO} which matches the criteria from the database
     *
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching entities.
     */
    @FindWithPermission("TASK.GET_LIST")
    @Transactional(readOnly = true)
    @PrivilegeDescription("Privilege to get all scheduled tasks")
    public List<TaskDTO> findByCriteria(TaskCriteria criteria, String privilegeKey) {
        log.debug("find by criteria : {}", criteria);
        List<Task> result = permittedRepository.findWithPermission(Task.class, criteria, null, privilegeKey)
            .getContent();
        return taskMapper.toDto(result);
    }

    /**
     * Return a {@link Page} of {@link TaskDTO} which matches the criteria from the database
     *
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param pageable The page, which should be returned.
     * @return the matching entities.
     */
    @FindWithPermission("TASK.GET_LIST")
    @Transactional(readOnly = true)
    @PrivilegeDescription("Privilege to get all scheduled tasks")
    public Page<TaskDTO> findByCriteria(TaskCriteria criteria, Pageable pageable, String privilegeKey) {
        log.debug("find by criteria : {}, page: {}", criteria, pageable);
        Page<Task> result = permittedRepository.findWithPermission(Task.class, criteria, pageable, privilegeKey);
        return result.map(taskMapper::toDto);
    }
}
