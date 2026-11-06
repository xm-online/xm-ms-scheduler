package com.icthh.xm.ms.scheduler.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.icthh.xm.ms.scheduler.AbstractSpringBootTest;
import com.icthh.xm.ms.scheduler.domain.Task;
import com.icthh.xm.ms.scheduler.domain.enumeration.ScheduleType;
import com.icthh.xm.ms.scheduler.manager.SchedulingManager;
import com.icthh.xm.ms.scheduler.repository.TaskRepository;
import com.icthh.xm.ms.scheduler.service.mapper.TaskMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class SchedulingPersistenceIntTest extends AbstractSpringBootTest {

    private static final String KEY = "persistence-test";

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private SystemTaskService systemTaskService;

    @Autowired
    private SchedulingManager schedulingManager;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private EntityManager em;

    @Test
    public void activeTasksIncludeNullEndDateAndExcludeExpiredAndDone() {
        Instant past = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant future = Instant.now().plus(1, ChronoUnit.HOURS);
        Long activeWithoutEnd = save("ACTIVE", null, null).getId();
        save(null, past, null);
        save("DONE", future, null);
        Long withoutState = save(null, future, null).getId();

        List<Long> active = systemTaskService.findUserNotFinishedTasks().stream()
            .filter(t -> KEY.equals(t.getKey())).map(t -> t.getId()).toList();

        assertThat(active).containsExactlyInAnyOrder(activeWithoutEnd, withoutState);
    }

    @Test
    public void oneTimeEndDateIsBackfilledOnlyWhenNull() {
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MILLIS);
        Instant storedEnd = start.plus(5, ChronoUnit.DAYS);
        Task withEnd = save(null, storedEnd, start);
        Task withoutEnd = save(null, null, start);

        schedulingManager.createOrUpdateActiveUserTask(taskMapper.toDto(withEnd));
        schedulingManager.createOrUpdateActiveUserTask(taskMapper.toDto(withoutEnd));
        em.flush();
        em.clear();

        assertThat(taskRepository.findById(withEnd.getId()).orElseThrow().getEndDate()).isEqualTo(storedEnd);
        assertThat(taskRepository.findById(withoutEnd.getId()).orElseThrow().getEndDate())
            .isEqualTo(start.plus(1, ChronoUnit.HOURS));
        schedulingManager.deleteActiveTask(withEnd.getId().toString());
        schedulingManager.deleteActiveTask(withoutEnd.getId().toString());
    }

    private Task save(String stateKey, Instant endDate, Instant oneTimeStart) {
        Task task = new Task().key(KEY).typeKey("T").createdBy("test").stateKey(stateKey).endDate(endDate);
        if (oneTimeStart != null) {
            task.scheduleType(ScheduleType.ONE_TIME).startDate(oneTimeStart);
        } else {
            task.scheduleType(ScheduleType.FIXED_RATE).delay(3_600_000L).startDate(Instant.now().plusSeconds(3600));
        }
        return taskRepository.saveAndFlush(task);
    }
}
