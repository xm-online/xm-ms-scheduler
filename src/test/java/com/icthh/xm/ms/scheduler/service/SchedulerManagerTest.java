package com.icthh.xm.ms.scheduler.service;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.when;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.icthh.xm.ms.scheduler.domain.enumeration.Scheduletype;
import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;
import com.icthh.xm.ms.scheduler.service.impl.TaskServiceExtImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
//    SchedulingConfiguration.class,
    SchedulerManagerTest.TestConfiguration.class
})
@Slf4j
public class SchedulerManagerTest {

    private AtomicLong aLong = new AtomicLong();

    private Multiset<Long> expiredTasks = HashMultiset.create();
    private Multiset<Long> executedTasks = HashMultiset.create();

    private SchedulingManager schedulingManager;

    @Mock
    private TaskServiceExtImpl taskServiceExt;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    @Before
    public void init() {

        MockitoAnnotations.initMocks(this);

        schedulingManager = new SchedulingManager(taskScheduler,
                                                  taskServiceExt,
                                                  executed -> executedTasks.add(executed.getId()),
                                                  expired -> expiredTasks.add(expired.getId()));
    }

    private TaskDTO createTaskFixedDelay(Long delay, Instant startDate, Instant endDate) {
        return createTask(Scheduletype.FIXED_DELAY, delay, null, startDate, endDate);
    }

    private TaskDTO createTaskFixedRate(Long delay, Instant startDate, Instant endDate) {
        return createTask(Scheduletype.FIXED_RATE, delay, null, startDate, endDate);
    }

    private TaskDTO createTaskByCron(String cron, Instant startDate, Instant endDate) {
        return createTask(Scheduletype.CRON, null, cron, startDate, endDate);
    }

    private TaskDTO createTask(Scheduletype type, Long delay, String cron, Instant startDate, Instant endDate) {
        TaskDTO dto = new TaskDTO();
        dto.setId(aLong.incrementAndGet());
        dto.setScheduletype(type);
        dto.setDelay(delay);
        dto.setClonExpression(cron);
        dto.setStartDate(startDate);
        dto.setEndDate(endDate);
        return dto;
    }

    @Test
    public void testInitFixedDelayTasks() {

        TaskDTO task = createTaskFixedDelay(300L, null, null);

        initScheduling(task);

        waitAndDeleteTask(300 * 5, task);

        expectRunAndExpiryCounts(task, 5, 0);

    }

    @Test
    public void testInitFixedRateTasks() {

        TaskDTO task = createTaskFixedRate(100L, null, null);

        initScheduling(task);

        // due to fixed rate we need to delete task little bit earlier
        waitAndDeleteTask(495, task);

        expectRunAndExpiryCounts(task, 5, 0);

    }

    @Test
    public void testInitCronTasks() {

        TaskDTO task = createTaskByCron("0/1 * * * * ?", null, null);

        initScheduling(task);

        // due to fixed rate we need to delete task little bit earlier
        waitAndDeleteTask(4000, task);

        expectRunAndExpiryCounts(task, 4, 0);

    }

    @Test
    public void testInitFixedDelayTasksWithExpiration() {

        TaskDTO task = createTaskFixedDelay(500L, null, Instant.now().plusSeconds(2));

        initScheduling(task);

        waitAndDeleteTask(3000, task);

        expectRunAndExpiryCounts(task, 4, 1);

    }

    @Test
    public void testInitFixedDelayTasksWithInitialDelayAndExpiration() {

        TaskDTO task = createTaskFixedDelay(500L, Instant.now().plusSeconds(1), Instant.now().plusSeconds(2));

        initScheduling(task);

        waitAndDeleteTask(3500, task);

        expectRunAndExpiryCounts(task, 2, 1);

    }

    @Test
    public void testInitConcurrentTasksWithExpiration() {

        TaskDTO task1 = createTaskFixedDelay(500L, null, Instant.now().plusSeconds(2));
        TaskDTO task2 = createTaskFixedDelay(500L, null, Instant.now().plusSeconds(2));

        initScheduling(task1, task2);

        waitAndDeleteTask(3000, task1, task2);

        expectRunAndExpiryCounts(task1, 4, 1);
        expectRunAndExpiryCounts(task2, 4, 1);

    }

    @Test
    public void testCreateTask() {

        TaskDTO task1 = createTaskFixedDelay(500L, null, Instant.now().plusSeconds(2));

        initScheduling();

        schedulingManager.updateActiveTask(task1);

        waitAndDeleteTask(3000, task1);

        expectRunAndExpiryCounts(task1, 4, 1);

    }

    @Test
    public void testUpdateTask() {

        TaskDTO task1 = createTaskFixedDelay(500L, Instant.now().plusMillis(20), null);

        initScheduling();

        // create task
        schedulingManager.updateActiveTask(task1);

        waitFor(2000);
        expectRunAndExpiryCounts(task1, 4, 0);

        // update existing task
        task1.setDelay(250L);
        task1.setEndDate(Instant.now().plusSeconds(2));
        schedulingManager.updateActiveTask(task1);

        waitFor(2000);
        expectRunAndExpiryCounts(task1, 4 + 8, 1);

    }

    private void waitFor(long wait) {
        try {
            System.out.println("##### wait for " + wait + " ms...");
            Thread.sleep(wait);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    private void waitAndDeleteTask(long wait, TaskDTO... tasks) {
        waitFor(wait);

        Arrays.stream(tasks).forEach(task -> {
            schedulingManager.deleteActiveTask(task.getId().toString());
        });

    }

    private void initScheduling(TaskDTO... tasks) {
        when(taskServiceExt.findAllNotFinishedTasks()).thenReturn(asList(tasks));
        schedulingManager.init();
    }

    private void expectRunAndExpiryCounts(TaskDTO task, int runCount, int expirycount) {
        Assert.assertEquals(runCount, executedTasks.count(task.getId()));
        Assert.assertEquals(expirycount, expiredTasks.count(task.getId()));
    }

    @Configuration
//    @ComponentScan(basePackages = {"com.icthh.xm.ms.scheduler.repository"})
//    @EnableAutoConfiguration
    public static class TestConfiguration {

//        @Autowired
//        TaskMapper taskMapper = Mappers.getMapper(TaskMapper.class);

//        @Bean(initMethod = "init")
//        public SchedulingManager schedulingManager(ThreadPoolTaskScheduler threadPoolTaskScheduler,
//                                                   @Qualifier("taskSrv") TaskServiceExtImpl taskServiceExt) {
//            return new SchedulingManager(threadPoolTaskScheduler, taskServiceExt);
//        }

        @Bean
        public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
            ThreadPoolTaskScheduler threadPoolTaskScheduler
                = new ThreadPoolTaskScheduler();
            threadPoolTaskScheduler.setPoolSize(5);
            threadPoolTaskScheduler.setThreadNamePrefix("xm-sc-thread");
            return threadPoolTaskScheduler;
        }

//        @Bean
//        public TaskMapper taskMapper() {
//            return Mappers.getMapper(TaskMapper.class);
//        }
//
//        @Bean(name = "taskSrv")
//        public TaskServiceExtImpl taskServiceExt(TaskMapper taskMapper) {
//            return new TaskServiceExtImpl(null, taskMapper);
//        }

    }

}
