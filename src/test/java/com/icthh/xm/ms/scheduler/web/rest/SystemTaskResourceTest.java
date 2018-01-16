package com.icthh.xm.ms.scheduler.web.rest;

import static com.icthh.xm.ms.scheduler.web.rest.TestUtil.createFormattingConversionService;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.icthh.xm.ms.scheduler.SchedulerApp;
import com.icthh.xm.ms.scheduler.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.ms.scheduler.domain.enumeration.ChannelType;
import com.icthh.xm.ms.scheduler.domain.enumeration.ScheduleType;
import com.icthh.xm.ms.scheduler.service.TaskServiceExt;
import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;
import com.icthh.xm.ms.scheduler.web.rest.errors.ExceptionTranslator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {SchedulerApp.class, SecurityBeanOverrideConfiguration.class})
public class SystemTaskResourceTest {

    private static final String DEFAULT_KEY = "systask1";
    private static final String DEFAULT_NAME = null;
    private static final String DEFAULT_TYPE_KEY = "SYSTEM.TASK1";
    private static final String DEFAULT_STATE_KEY = null;
    private static final String DEFAULT_CREATED_BY = null;
    private static final Instant DEFAULT_START_DATE = null;
    private static final Instant DEFAULT_END_DATE = null;
    private static final ScheduleType DEFAULT_SCHEDULE_TYPE = ScheduleType.FIXED_DELAY;
    private static final Long DEFAULT_DELAY = 1000L;
    private static final String DEFAULT_CRON_EXPRESSION = null;
    private static final ChannelType DEFAULT_CHANNEL_TYPE = null;
    private static final String DEFAULT_DESCRIPTION = null;
    private static final String DEFAULT_DATA = null;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private TaskServiceExt taskServiceExt;

    private MockMvc restTaskMockMvc;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final SystemTaskResource taskResource = new SystemTaskResource(taskServiceExt);

        this.restTaskMockMvc = MockMvcBuilders.standaloneSetup(taskResource)
                                              .setControllerAdvice(exceptionTranslator)
                                              .setConversionService(createFormattingConversionService())
                                              .setMessageConverters(jacksonMessageConverter).build();
    }

    @Test
    @Transactional
    public void getTask() throws Exception {

        TaskDTO task = taskServiceExt.findOneTaskFromConfigByKey("systask1");

        // Get the task
        restTaskMockMvc.perform(get("/api/systasks/{key}", task.getKey()))
                       .andExpect(status().isOk())
                       .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                       .andExpect(jsonPath("$.id").value(nullValue()))
                       .andExpect(jsonPath("$.key").value(DEFAULT_KEY))
                       .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
                       .andExpect(jsonPath("$.typeKey").value(DEFAULT_TYPE_KEY))
                       .andExpect(jsonPath("$.stateKey").value(DEFAULT_STATE_KEY))
                       .andExpect(jsonPath("$.createdBy").value(DEFAULT_CREATED_BY))
                       .andExpect(jsonPath("$.startDate").value(DEFAULT_START_DATE))
                       .andExpect(jsonPath("$.endDate").value(DEFAULT_END_DATE))
                       .andExpect(jsonPath("$.scheduleType").value(DEFAULT_SCHEDULE_TYPE.toString()))
                       .andExpect(jsonPath("$.delay").value(DEFAULT_DELAY.intValue()))
                       .andExpect(jsonPath("$.cronExpression").value(DEFAULT_CRON_EXPRESSION))
                       .andExpect(jsonPath("$.channelType").value(DEFAULT_CHANNEL_TYPE))
                       .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION))
                       .andExpect(jsonPath("$.data").value(DEFAULT_DATA));
    }

    @Test
    @Transactional
    public void getAllTasks() throws Exception {

        // Get all the taskList
        restTaskMockMvc.perform(get("/api/systasks"))
                       .andExpect(status().isOk())
                       .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                       .andExpect(jsonPath("$.[*].id").value(hasItem(nullValue())))
                       .andExpect(jsonPath("$.[*].key").value(hasItem(DEFAULT_KEY)))
                       .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
                       .andExpect(jsonPath("$.[*].typeKey").value(hasItem(DEFAULT_TYPE_KEY)))
                       .andExpect(jsonPath("$.[*].stateKey").value(hasItem(DEFAULT_STATE_KEY)))
                       .andExpect(jsonPath("$.[*].createdBy").value(hasItem(DEFAULT_CREATED_BY)))
                       .andExpect(jsonPath("$.[*].startDate").value(hasItem(DEFAULT_START_DATE)))
                       .andExpect(jsonPath("$.[*].endDate").value(hasItem(DEFAULT_END_DATE)))
                       .andExpect(jsonPath("$.[*].scheduleType").value(hasItem(DEFAULT_SCHEDULE_TYPE.toString())))
                       .andExpect(jsonPath("$.[*].delay").value(hasItem(DEFAULT_DELAY.intValue())))
                       .andExpect(jsonPath("$.[*].cronExpression").value(hasItem(DEFAULT_CRON_EXPRESSION)))
                       .andExpect(jsonPath("$.[*].channelType").value(hasItem(DEFAULT_CHANNEL_TYPE)))
                       .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
                       .andExpect(jsonPath("$.[*].data").value(hasItem(DEFAULT_DATA)));
    }

}
