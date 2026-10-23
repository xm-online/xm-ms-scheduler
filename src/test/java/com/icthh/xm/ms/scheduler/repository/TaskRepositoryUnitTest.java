package com.icthh.xm.ms.scheduler.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.icthh.xm.ms.scheduler.AbstractSpringBootTest;
import com.icthh.xm.ms.scheduler.domain.Task;
import com.icthh.xm.ms.scheduler.domain.enumeration.ScheduleType;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** The active query matrix (trap 8) and the targeted updates. */
@Transactional
public class TaskRepositoryUnitTest extends AbstractSpringBootTest {

    @Autowired
    private TaskRepository repository;

    @Autowired
    private EntityManager em;

    private Task save(String stateKey, Instant endDate) {
        Task task = new Task().typeKey("T").createdBy("test").scheduleType(ScheduleType.FIXED_DELAY).delay(1000L)
            .stateKey(stateKey).endDate(endDate);
        return repository.saveAndFlush(task);
    }

    private Set<Long> activeIds() {
        return repository.findActive(Instant.now()).stream().map(Task::getId).collect(Collectors.toSet());
    }

    @Test
    public void activeWithNullEndDateIsLoaded() {
        Task task = save("ACTIVE", null);
        assertThat(activeIds()).contains(task.getId());
    }

    @Test
    public void doneIsNotLoaded() {
        Task task = save("DONE", Instant.now().plus(1, ChronoUnit.HOURS));
        Task doneWithoutEnd = save("DONE", null);
        assertThat(activeIds()).doesNotContain(task.getId(), doneWithoutEnd.getId());
    }

    @Test
    public void pastEndDateIsNotLoaded() {
        Task task = save("ACTIVE", Instant.now().minus(1, ChronoUnit.HOURS));
        assertThat(activeIds()).doesNotContain(task.getId());
    }

    @Test
    public void nullStateWithFutureEndDateIsLoaded() {
        Task task = save(null, Instant.now().plus(1, ChronoUnit.HOURS));
        assertThat(activeIds()).contains(task.getId());
    }

    @Test
    public void nullStateWithPastEndDateIsNotLoaded() {
        Task task = save(null, Instant.now().minus(1, ChronoUnit.HOURS));
        assertThat(activeIds()).doesNotContain(task.getId());
    }

    @Test
    public void backfillEndDateDoesNotOverwrite() {
        Instant existing = Instant.now().plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MILLIS);
        Task withEnd = save("ACTIVE", existing);
        Task withoutEnd = save("ACTIVE", null);
        Instant filled = Instant.now().plus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MILLIS);

        assertThat(repository.backfillEndDate(withEnd.getId(), filled)).isZero();
        assertThat(repository.backfillEndDate(withoutEnd.getId(), filled)).isEqualTo(1);
        em.clear();

        assertThat(repository.findById(withEnd.getId()).orElseThrow().getEndDate()).isEqualTo(existing);
        assertThat(repository.findById(withoutEnd.getId()).orElseThrow().getEndDate()).isEqualTo(filled);
    }

    @Test
    public void updateStateKeyChangesOnlyTheState() {
        Instant end = Instant.now().plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MILLIS);
        Task task = save("ACTIVE", end);
        assertThat(repository.updateStateKey(task.getId(), "DONE")).isEqualTo(1);
        em.clear();

        Task loaded = repository.findById(task.getId()).orElseThrow();
        assertThat(loaded.getStateKey()).isEqualTo("DONE");
        assertThat(loaded.getEndDate()).isEqualTo(end);
        assertThat(loaded.getDelay()).isEqualTo(1000L);
    }
}
