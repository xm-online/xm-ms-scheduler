package com.icthh.xm.ms.scheduler.repository;

import com.icthh.xm.ms.scheduler.domain.Task;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Task entity.
 */
@SuppressWarnings("unused")
@Repository
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    List<Task> findByEndDateGreaterThanEqualAndStateKeyNotOrStateKeyNull(Instant endDate, String stateKey);

}
