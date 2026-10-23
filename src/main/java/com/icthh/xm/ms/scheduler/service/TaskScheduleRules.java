package com.icthh.xm.ms.scheduler.service;

import static java.time.temporal.ChronoUnit.HOURS;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;

import java.time.Instant;

import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.support.CronExpression;

/**
 * Schedule rules shared by save (validation) and the scheduler.
 */
public final class TaskScheduleRules {

    public static final String ERROR_CODE = "error.task.schedule.invalid";

    private TaskScheduleRules() {
    }

    /** Throws a BusinessException (HTTP 400) when the task cannot be scheduled. */
    public static void validate(TaskDTO task) {
        if (task.getScheduleType() == null) {
            invalid("scheduleType", "is required");
        }
        switch (task.getScheduleType()) {
            case FIXED_DELAY:
            case FIXED_RATE:
                if (task.getDelay() == null || task.getDelay() <= 0) {
                    invalid("delay", "must be greater than 0 for " + task.getScheduleType());
                }
                break;
            case CRON:
                if (task.getCronExpression() == null || !CronExpression.isValidExpression(task.getCronExpression())) {
                    invalid("cronExpression", "is missing or not a valid cron expression");
                }
                rejectTimeZone(task.getCronTriggerTimeZoneId());
                break;
            case ONE_TIME:
                if (task.getStartDate() == null) {
                    invalid("startDate", "is required for ONE_TIME");
                }
                break;
            default:
                break;
        }
    }

    /** The zone is not persisted (no column), so a user task with a zone would silently run in the server zone. */
    private static void rejectTimeZone(String zoneId) {
        if (StringUtils.isNotBlank(zoneId)) {
            invalid("cronTriggerTimeZoneId", "time zones are not supported yet");
        }
    }

    /**
     * End date of a ONE_TIME task: start plus ttl seconds, or one hour without ttl. This is master's formula: master's
     * scheduleOneTimeTask set it that way and saved it when the task was scheduled; it is now computed at save.
     */
    public static Instant oneTimeEndDate(Instant startDate, Integer ttl) {
        return ttl != null ? startDate.plusSeconds(ttl) : startDate.plus(1, HOURS);
    }

    private static void invalid(String field, String reason) {
        throw new BusinessException(ERROR_CODE, field + ": " + reason);
    }
}
