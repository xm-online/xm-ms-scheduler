package com.icthh.xm.ms.scheduler;

import com.icthh.xm.ms.scheduler.domain.enumeration.ScheduleType;
import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 */
public class TaskTestUtil {

    private static AtomicLong aLong = new AtomicLong();

    private TaskTestUtil() {

    }

    public static TaskDTO createTask(ScheduleType type, Long delay, String cron, Instant startDate, Instant endDate) {
        TaskDTO dto = new TaskDTO();
        dto.setId(aLong.incrementAndGet());
        dto.setScheduleType(type);
        dto.setDelay(delay);
        dto.setCronExpression(cron);
        dto.setStartDate(startDate);
        dto.setEndDate(endDate);
        return dto;
    }

    public static TaskDTO createTaskFixedDelay(Long delay, Instant startDate, Instant endDate) {
        return createTask(ScheduleType.FIXED_DELAY, delay, null, startDate, endDate);
    }

    public static TaskDTO createTaskFixedRate(Long delay, Instant startDate, Instant endDate) {
        return createTask(ScheduleType.FIXED_RATE, delay, null, startDate, endDate);
    }

    public static TaskDTO createTaskByCron(String cron, Instant startDate, Instant endDate) {
        return createTask(ScheduleType.CRON, null, cron, startDate, endDate);
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
