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

    /** Active task: not DONE and not expired. A null end date never expires. */
    String ACTIVE = "(t.stateKey is null or t.stateKey <> 'DONE') and (t.endDate is null or t.endDate >= :now)";

    @Query("select t from Task t where " + ACTIVE)
    List<Task> findActive(@Param("now") Instant now);

    @Transactional
    @Modifying
    @Query("update Task t set t.stateKey = :state where t.id = :id")
    int updateStateKey(@Param("id") Long id, @Param("state") String state);

    @Transactional
    @Modifying
    @Query("update Task t set t.endDate = :endDate where t.id = :id and t.endDate is null")
    int backfillEndDate(@Param("id") Long id, @Param("endDate") Instant endDate);

}
