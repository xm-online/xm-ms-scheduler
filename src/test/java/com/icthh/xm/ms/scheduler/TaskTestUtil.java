package com.icthh.xm.ms.scheduler;

import com.icthh.xm.ms.scheduler.domain.enumeration.ScheduleType;
import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility class necessary for testing.
 */
public class TaskTestUtil {

    private static AtomicLong aLong = new AtomicLong();

    public static final String TEST_TENANT = "TEST";
    public static final String XM_TENANT = "XM";

    private TaskTestUtil() {

    }

    public static TaskDTO createTask(ScheduleType type,
                                     Long delay,
                                     String cron,
                                     Instant startDate,
                                     Instant endDate,
                                     Integer ttl,
                                     String cronTriggerTimeZoneId) {
        TaskDTO dto = new TaskDTO();
        dto.setId(aLong.incrementAndGet());
        dto.setScheduleType(type);
        dto.setDelay(delay);
        dto.setCronExpression(cron);
        dto.setStartDate(startDate);
        dto.setEndDate(endDate);
        dto.setTtl(ttl);
        dto.setCronTriggerTimeZoneId(cronTriggerTimeZoneId);
        return dto;
    }

    public static TaskDTO createTaskFixedDelay(Long delay, Instant startDate, Instant endDate) {
        return createTask(ScheduleType.FIXED_DELAY, delay, null, startDate, endDate, null, null);
    }

    public static TaskDTO createTaskFixedRate(Long delay, Instant startDate, Instant endDate) {
        return createTask(ScheduleType.FIXED_RATE, delay, null, startDate, endDate, null, null);
    }

    public static TaskDTO createTaskByCron(String cron, Instant startDate, Instant endDate) {
        return createTask(ScheduleType.CRON, null, cron, startDate, endDate, null, null);
    }

    public static TaskDTO createTaskByCron(String cron, String cronTriggerTimeZoneId) {
        return createTask(ScheduleType.CRON, null, cron, null, null, null, cronTriggerTimeZoneId);
    }

    public static TaskDTO createTaskOneTime(Instant startDate, Integer ttl) {
        return createTask(ScheduleType.ONE_TIME, null, null, startDate, null, ttl, null);
    }

    public static void waitFor(long wait) {
        try {
            System.out.println("##### wait for " + wait + " ms...");
            Thread.sleep(wait);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}
