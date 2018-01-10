package com.icthh.xm.ms.scheduler.repository;

import com.icthh.xm.ms.scheduler.domain.Task;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.*;

import java.time.Instant;
import java.util.List;

/**
 * Spring Data JPA repository for the Task entity.
 */
@SuppressWarnings("unused")
@Repository
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    List<Task> findByEndDateGreaterThanEqual(Instant endDate);

}
