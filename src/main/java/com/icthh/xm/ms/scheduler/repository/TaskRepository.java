package com.icthh.xm.ms.scheduler.repository;

import com.icthh.xm.ms.scheduler.domain.Task;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Data JPA repository for the Task entity.
 */
@SuppressWarnings("unused")
@Repository
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    /** JPQL condition of a task that is not done and not past its end date; needs the parameter {@code :now}. */
    String ACTIVE_TASK_CONDITION =
        "(t.stateKey IS NULL OR t.stateKey <> 'DONE') AND (t.endDate IS NULL OR t.endDate >= :now)";

    @Query("SELECT t FROM Task t WHERE " + ACTIVE_TASK_CONDITION)
    List<Task> findActive(@Param("now") Instant now);

    @Transactional
    @Modifying
    @Query("UPDATE Task t SET t.stateKey = :stateKey WHERE t.id = :id AND t.startDate = :startDate")
    int updateStateKey(@Param("id") Long id, @Param("stateKey") String stateKey,
                       @Param("startDate") Instant startDate);

    @Transactional
    @Modifying
    @Query("UPDATE Task t SET t.endDate = :endDate WHERE t.id = :id AND t.endDate IS NULL")
    int backfillEndDate(@Param("id") Long id, @Param("endDate") Instant endDate);

}
