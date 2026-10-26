package com.icthh.xm.ms.scheduler.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.ms.scheduler.AbstractUnitTest;
import com.icthh.xm.ms.scheduler.domain.enumeration.ScheduleType;
import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;

import java.time.Instant;
import org.junit.jupiter.api.Test;

public class TaskScheduleRulesUnitTest extends AbstractUnitTest {

    private static TaskDTO task(ScheduleType type, Long delay, String cron, Instant start) {
        TaskDTO dto = new TaskDTO();
        dto.setScheduleType(type);
        dto.setDelay(delay);
        dto.setCronExpression(cron);
        dto.setStartDate(start);
        return dto;
    }

    @Test
    public void validTasksPass() {
        assertThatCode(() -> {
            TaskScheduleRules.validate(task(ScheduleType.FIXED_DELAY, 1L, null, null));
            TaskScheduleRules.validate(task(ScheduleType.FIXED_RATE, 10L, null, null));
            TaskScheduleRules.validate(task(ScheduleType.CRON, null, "0/1 * * * * ?", null));
            TaskScheduleRules.validate(task(ScheduleType.ONE_TIME, null, null, Instant.now()));
        }).doesNotThrowAnyException();
    }

    @Test
    public void invalidTasksAreRejected() {
        assertInvalid(task(null, 1L, null, null));
        assertInvalid(task(ScheduleType.FIXED_DELAY, null, null, null));
        assertInvalid(task(ScheduleType.FIXED_RATE, 0L, null, null));
        assertInvalid(task(ScheduleType.CRON, null, null, null));
        assertInvalid(task(ScheduleType.CRON, null, "not a cron", null));
        assertInvalid(task(ScheduleType.ONE_TIME, null, null, null));
    }

    @Test
    public void nonBlankTimeZoneIsRejectedAndBlankPasses() {
        for (String zone : new String[] {"Mars/Olympus", "Europe/Kyiv"}) {
            TaskDTO withZone = task(ScheduleType.CRON, null, "0/1 * * * * ?", null);
            withZone.setCronTriggerTimeZoneId(zone);
            assertThatThrownBy(() -> TaskScheduleRules.validate(withZone)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("time zones are not supported yet");
        }

        TaskDTO blank = task(ScheduleType.CRON, null, "0/1 * * * * ?", null);
        blank.setCronTriggerTimeZoneId(" ");
        assertThatCode(() -> TaskScheduleRules.validate(blank)).doesNotThrowAnyException();
    }

    private void assertInvalid(TaskDTO dto) {
        assertThatThrownBy(() -> TaskScheduleRules.validate(dto)).isInstanceOf(BusinessException.class);
    }

    @Test
    public void oneTimeEndDate() {
        Instant start = Instant.parse("2030-01-01T00:00:00Z");
        assertThat(TaskScheduleRules.oneTimeEndDate(start, 30)).isEqualTo(start.plusSeconds(30));
        assertThat(TaskScheduleRules.oneTimeEndDate(start, null)).isEqualTo(start.plusSeconds(3600));
    }
}
