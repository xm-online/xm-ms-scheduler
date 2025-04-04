package com.icthh.xm.ms.scheduler.web.rest;

import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.ms.scheduler.AbstractSpringBootTest;
import com.icthh.xm.ms.scheduler.domain.Task;
import com.icthh.xm.ms.scheduler.domain.enumeration.ChannelType;
import com.icthh.xm.ms.scheduler.domain.enumeration.ScheduleType;
import com.icthh.xm.ms.scheduler.repository.TaskRepository;
import com.icthh.xm.ms.scheduler.service.TaskQueryService;
import com.icthh.xm.ms.scheduler.service.TaskService;
import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;
import com.icthh.xm.ms.scheduler.service.mapper.TaskMapper;
import jakarta.persistence.EntityManager;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test class for the TaskResource REST controller.
 *
 * @see TaskResource
 */

@WithMockUser(authorities = {"SUPER-ADMIN"})
public class TaskResourceTest extends AbstractSpringBootTest {

    private static final String DEFAULT_KEY = "AAAAAAAAAA";
    private static final String UPDATED_KEY = "BBBBBBBBBB";

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_TYPE_KEY = "AAAAAAAAAA";
    private static final String UPDATED_TYPE_KEY = "BBBBBBBBBB";

    private static final String DEFAULT_STATE_KEY = "AAAAAAAAAA";
    private static final String UPDATED_STATE_KEY = "BBBBBBBBBB";

    private static final String DEFAULT_CREATED_BY = "AAAAAAAAAA";
    private static final String UPDATED_CREATED_BY = "BBBBBBBBBB";

    private static final Instant DEFAULT_START_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_START_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_END_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_END_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final ScheduleType DEFAULT_SCHEDULE_TYPE = ScheduleType.FIXED_RATE;
    private static final ScheduleType UPDATED_SCHEDULE_TYPE = ScheduleType.FIXED_DELAY;

    private static final Long DEFAULT_DELAY = 1L;
    private static final Long UPDATED_DELAY = 2L;

    private static final String DEFAULT_CRON_EXPRESSION = "AAAAAAAAAA";
    private static final String UPDATED_CRON_EXPRESSION = "BBBBBBBBBB";

    private static final ChannelType DEFAULT_CHANNEL_TYPE = ChannelType.QUEUE;
    private static final ChannelType UPDATED_CHANNEL_TYPE = ChannelType.TOPIC;

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final String DEFAULT_DATA = "AAAAAAAAAA";
    private static final String UPDATED_DATA = "BBBBBBBBBB";

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskQueryService taskQueryService;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    private MockMvc restTaskMockMvc;

    private Task task;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final TaskResource taskResource = new TaskResource(taskService, taskQueryService);
        this.restTaskMockMvc = MockMvcBuilders.standaloneSetup(taskResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(TestUtil.createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter)
            .build();
    }

    /**
     * Create an entity for this test.
     * <p>
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Task createEntity(EntityManager em) {
        Task task = new Task()
            .key(DEFAULT_KEY)
            .name(DEFAULT_NAME)
            .typeKey(DEFAULT_TYPE_KEY)
            .stateKey(DEFAULT_STATE_KEY)
            .createdBy(DEFAULT_CREATED_BY)
            .startDate(DEFAULT_START_DATE)
            .endDate(DEFAULT_END_DATE)
            .scheduleType(DEFAULT_SCHEDULE_TYPE)
            .delay(DEFAULT_DELAY)
            .cronExpression(DEFAULT_CRON_EXPRESSION)
            .channelType(DEFAULT_CHANNEL_TYPE)
            .description(DEFAULT_DESCRIPTION)
            .data(DEFAULT_DATA);
        return task;
    }

    @Before
    public void initTest() {
        task = createEntity(em);
    }

    @Test
    @Transactional
    public void createTask() throws Exception {
        int databaseSizeBeforeCreate = taskRepository.findAll().size();

        // Create the Task
        TaskDTO taskDTO = taskMapper.toDto(task);
        restTaskMockMvc.perform(post("/api/tasks")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(taskDTO)))
            .andExpect(status().isCreated());

        // Validate the Task in the database
        List<Task> taskList = taskRepository.findAll();
        assertThat(taskList).hasSize(databaseSizeBeforeCreate + 1);
        Task testTask = taskList.get(taskList.size() - 1);
        assertThat(testTask.getKey()).isEqualTo(DEFAULT_KEY);
        assertThat(testTask.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testTask.getTypeKey()).isEqualTo(DEFAULT_TYPE_KEY);
        assertThat(testTask.getStateKey()).isEqualTo(DEFAULT_STATE_KEY);
        assertThat(testTask.getCreatedBy()).isEqualTo(DEFAULT_CREATED_BY);
        assertThat(testTask.getStartDate()).isEqualTo(DEFAULT_START_DATE);
        assertThat(testTask.getEndDate()).isEqualTo(DEFAULT_END_DATE);
        assertThat(testTask.getScheduleType()).isEqualTo(DEFAULT_SCHEDULE_TYPE);
        assertThat(testTask.getDelay()).isEqualTo(DEFAULT_DELAY);
        assertThat(testTask.getCronExpression()).isEqualTo(DEFAULT_CRON_EXPRESSION);
        assertThat(testTask.getChannelType()).isEqualTo(DEFAULT_CHANNEL_TYPE);
        assertThat(testTask.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(testTask.getData()).isEqualTo(DEFAULT_DATA);
    }

    @Test
    @Transactional
    public void createTaskWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = taskRepository.findAll().size();

        // Create the Task with an existing ID
        task.setId(1L);
        TaskDTO taskDTO = taskMapper.toDto(task);

        // An entity with an existing ID cannot be created, so this API call must fail
        restTaskMockMvc.perform(post("/api/tasks")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(taskDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Task in the database
        List<Task> taskList = taskRepository.findAll();
        assertThat(taskList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkTypeKeyIsRequired() throws Exception {
        int databaseSizeBeforeTest = taskRepository.findAll().size();
        // set the field null
        task.setTypeKey(null);

        // Create the Task, which fails.
        TaskDTO taskDTO = taskMapper.toDto(task);

        restTaskMockMvc.perform(post("/api/tasks")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(taskDTO)))
            .andExpect(status().isBadRequest());

        List<Task> taskList = taskRepository.findAll();
        assertThat(taskList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkCreatedByIsRequired() throws Exception {
        int databaseSizeBeforeTest = taskRepository.findAll().size();
        // set the field null
        task.setCreatedBy(null);

        // Create the Task, which fails.
        TaskDTO taskDTO = taskMapper.toDto(task);

        restTaskMockMvc.perform(post("/api/tasks")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(taskDTO)))
            .andExpect(status().isBadRequest());

        List<Task> taskList = taskRepository.findAll();
        assertThat(taskList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllTasks() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList
        restTaskMockMvc.perform(get("/api/tasks?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(task.getId().intValue())))
            .andExpect(jsonPath("$.[*].key").value(hasItem(DEFAULT_KEY.toString())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME.toString())))
            .andExpect(jsonPath("$.[*].typeKey").value(hasItem(DEFAULT_TYPE_KEY.toString())))
            .andExpect(jsonPath("$.[*].stateKey").value(hasItem(DEFAULT_STATE_KEY.toString())))
            .andExpect(jsonPath("$.[*].createdBy").value(hasItem(DEFAULT_CREATED_BY.toString())))
            .andExpect(jsonPath("$.[*].startDate").value(hasItem(DEFAULT_START_DATE.toString())))
            .andExpect(jsonPath("$.[*].endDate").value(hasItem(DEFAULT_END_DATE.toString())))
            .andExpect(jsonPath("$.[*].scheduleType").value(hasItem(DEFAULT_SCHEDULE_TYPE.toString())))
            .andExpect(jsonPath("$.[*].delay").value(hasItem(DEFAULT_DELAY.intValue())))
            .andExpect(jsonPath("$.[*].cronExpression").value(hasItem(DEFAULT_CRON_EXPRESSION.toString())))
            .andExpect(jsonPath("$.[*].channelType").value(hasItem(DEFAULT_CHANNEL_TYPE.toString())))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION.toString())))
            .andExpect(jsonPath("$.[*].data").value(hasItem(DEFAULT_DATA.toString())));
    }

    @Test
    @Transactional
    public void getTask() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get the task
        restTaskMockMvc.perform(get("/api/tasks/{id}", task.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(task.getId().intValue()))
            .andExpect(jsonPath("$.key").value(DEFAULT_KEY.toString()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME.toString()))
            .andExpect(jsonPath("$.typeKey").value(DEFAULT_TYPE_KEY.toString()))
            .andExpect(jsonPath("$.stateKey").value(DEFAULT_STATE_KEY.toString()))
            .andExpect(jsonPath("$.createdBy").value(DEFAULT_CREATED_BY.toString()))
            .andExpect(jsonPath("$.startDate").value(DEFAULT_START_DATE.toString()))
            .andExpect(jsonPath("$.endDate").value(DEFAULT_END_DATE.toString()))
            .andExpect(jsonPath("$.scheduleType").value(DEFAULT_SCHEDULE_TYPE.toString()))
            .andExpect(jsonPath("$.delay").value(DEFAULT_DELAY.intValue()))
            .andExpect(jsonPath("$.cronExpression").value(DEFAULT_CRON_EXPRESSION.toString()))
            .andExpect(jsonPath("$.channelType").value(DEFAULT_CHANNEL_TYPE.toString()))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION.toString()))
            .andExpect(jsonPath("$.data").value(DEFAULT_DATA.toString()));
    }

    @Test
    @Transactional
    public void getAllTasksByKeyIsEqualToSomething() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList where key equals to DEFAULT_KEY
        defaultTaskShouldBeFound("key.equals=" + DEFAULT_KEY);

        // Get all the taskList where key equals to UPDATED_KEY
        defaultTaskShouldNotBeFound("key.equals=" + UPDATED_KEY);
    }

    @Test
    @Transactional
    public void getAllTasksByKeyIsInShouldWork() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList where key in DEFAULT_KEY or UPDATED_KEY
        defaultTaskShouldBeFound("key.in=" + DEFAULT_KEY + "," + UPDATED_KEY);

        // Get all the taskList where key equals to UPDATED_KEY
        defaultTaskShouldNotBeFound("key.in=" + UPDATED_KEY);
    }

    @Test
    @Transactional
    public void getAllTasksByKeyIsNullOrNotNull() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList where key is not null
        defaultTaskShouldBeFound("key.specified=true");

        // Get all the taskList where key is null
        defaultTaskShouldNotBeFound("key.specified=false");
    }

    @Test
    @Transactional
    public void getAllTasksByNameIsEqualToSomething() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList where name equals to DEFAULT_NAME
        defaultTaskShouldBeFound("name.equals=" + DEFAULT_NAME);

        // Get all the taskList where name equals to UPDATED_NAME
        defaultTaskShouldNotBeFound("name.equals=" + UPDATED_NAME);
    }

    @Test
    @Transactional
    public void getAllTasksByNameIsInShouldWork() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList where name in DEFAULT_NAME or UPDATED_NAME
        defaultTaskShouldBeFound("name.in=" + DEFAULT_NAME + "," + UPDATED_NAME);

        // Get all the taskList where name equals to UPDATED_NAME
        defaultTaskShouldNotBeFound("name.in=" + UPDATED_NAME);
    }

    @Test
    @Transactional
    public void getAllTasksByNameIsNullOrNotNull() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList where name is not null
        defaultTaskShouldBeFound("name.specified=true");

        // Get all the taskList where name is null
        defaultTaskShouldNotBeFound("name.specified=false");
    }

    @Test
    @Transactional
    public void getAllTasksByTypeKeyIsEqualToSomething() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList where typeKey equals to DEFAULT_TYPE_KEY
        defaultTaskShouldBeFound("typeKey.equals=" + DEFAULT_TYPE_KEY);

        // Get all the taskList where typeKey equals to UPDATED_TYPE_KEY
        defaultTaskShouldNotBeFound("typeKey.equals=" + UPDATED_TYPE_KEY);
    }

    @Test
    @Transactional
    public void getAllTasksByTypeKeyIsInShouldWork() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList where typeKey in DEFAULT_TYPE_KEY or UPDATED_TYPE_KEY
        defaultTaskShouldBeFound("typeKey.in=" + DEFAULT_TYPE_KEY + "," + UPDATED_TYPE_KEY);

        // Get all the taskList where typeKey equals to UPDATED_TYPE_KEY
        defaultTaskShouldNotBeFound("typeKey.in=" + UPDATED_TYPE_KEY);
    }

    @Test
    @Transactional
    public void getAllTasksByTypeKeyIsNullOrNotNull() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList where typeKey is not null
        defaultTaskShouldBeFound("typeKey.specified=true");

        // Get all the taskList where typeKey is null
        defaultTaskShouldNotBeFound("typeKey.specified=false");
    }

    @Test
    @Transactional
    public void getAllTasksByStateKeyIsEqualToSomething() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList where stateKey equals to DEFAULT_STATE_KEY
        defaultTaskShouldBeFound("stateKey.equals=" + DEFAULT_STATE_KEY);

        // Get all the taskList where stateKey equals to UPDATED_STATE_KEY
        defaultTaskShouldNotBeFound("stateKey.equals=" + UPDATED_STATE_KEY);
    }

    @Test
    @Transactional
    public void getAllTasksByStateKeyIsInShouldWork() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList where stateKey in DEFAULT_STATE_KEY or UPDATED_STATE_KEY
        defaultTaskShouldBeFound("stateKey.in=" + DEFAULT_STATE_KEY + "," + UPDATED_STATE_KEY);

        // Get all the taskList where stateKey equals to UPDATED_STATE_KEY
        defaultTaskShouldNotBeFound("stateKey.in=" + UPDATED_STATE_KEY);
    }

    @Test
    @Transactional
    public void getAllTasksByStateKeyIsNullOrNotNull() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList where stateKey is not null
        defaultTaskShouldBeFound("stateKey.specified=true");

        // Get all the taskList where stateKey is null
        defaultTaskShouldNotBeFound("stateKey.specified=false");
    }

    @Test
    @Transactional
    public void getAllTasksByCreatedByIsEqualToSomething() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList where createdBy equals to DEFAULT_CREATED_BY
        defaultTaskShouldBeFound("createdBy.equals=" + DEFAULT_CREATED_BY);

        // Get all the taskList where createdBy equals to UPDATED_CREATED_BY
        defaultTaskShouldNotBeFound("createdBy.equals=" + UPDATED_CREATED_BY);
    }

    @Test
    @Transactional
    public void getAllTasksByCreatedByIsInShouldWork() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList where createdBy in DEFAULT_CREATED_BY or UPDATED_CREATED_BY
        defaultTaskShouldBeFound("createdBy.in=" + DEFAULT_CREATED_BY + "," + UPDATED_CREATED_BY);

        // Get all the taskList where createdBy equals to UPDATED_CREATED_BY
        defaultTaskShouldNotBeFound("createdBy.in=" + UPDATED_CREATED_BY);
    }

    @Test
    @Transactional
    public void getAllTasksByCreatedByIsNullOrNotNull() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList where createdBy is not null
        defaultTaskShouldBeFound("createdBy.specified=true");

        // Get all the taskList where createdBy is null
        defaultTaskShouldNotBeFound("createdBy.specified=false");
    }

    @Test
    @Transactional
    public void getAllTasksByStartDateIsEqualToSomething() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList where startDate equals to DEFAULT_START_DATE
        defaultTaskShouldBeFound("startDate.equals=" + DEFAULT_START_DATE);

        // Get all the taskList where startDate equals to UPDATED_START_DATE
        defaultTaskShouldNotBeFound("startDate.equals=" + UPDATED_START_DATE);
    }

    @Test
    @Transactional
    public void getAllTasksByStartDateIsInShouldWork() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList where startDate in DEFAULT_START_DATE or UPDATED_START_DATE
        defaultTaskShouldBeFound("startDate.in=" + DEFAULT_START_DATE + "," + UPDATED_START_DATE);

        // Get all the taskList where startDate equals to UPDATED_START_DATE
        defaultTaskShouldNotBeFound("startDate.in=" + UPDATED_START_DATE);
    }

    @Test
    @Transactional
    public void getAllTasksByStartDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList where startDate is not null
        defaultTaskShouldBeFound("startDate.specified=true");

        // Get all the taskList where startDate is null
        defaultTaskShouldNotBeFound("startDate.specified=false");
    }

    @Test
    @Transactional
    public void getAllTasksByEndDateIsEqualToSomething() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList where endDate equals to DEFAULT_END_DATE
        defaultTaskShouldBeFound("endDate.equals=" + DEFAULT_END_DATE);

        // Get all the taskList where endDate equals to UPDATED_END_DATE
        defaultTaskShouldNotBeFound("endDate.equals=" + UPDATED_END_DATE);
    }

    @Test
    @Transactional
    public void getAllTasksByEndDateIsInShouldWork() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList where endDate in DEFAULT_END_DATE or UPDATED_END_DATE
        defaultTaskShouldBeFound("endDate.in=" + DEFAULT_END_DATE + "," + UPDATED_END_DATE);

        // Get all the taskList where endDate equals to UPDATED_END_DATE
        defaultTaskShouldNotBeFound("endDate.in=" + UPDATED_END_DATE);
    }

    @Test
    @Transactional
    public void getAllTasksByEndDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList where endDate is not null
        defaultTaskShouldBeFound("endDate.specified=true");

        // Get all the taskList where endDate is null
        defaultTaskShouldNotBeFound("endDate.specified=false");
    }

    @Test
    @Transactional
    public void getAllTasksByScheduleTypeIsEqualToSomething() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList where scheduleType equals to DEFAULT_SCHEDULE_TYPE
        defaultTaskShouldBeFound("scheduleType.equals=" + DEFAULT_SCHEDULE_TYPE);

        // Get all the taskList where scheduleType equals to UPDATED_SCHEDULE_TYPE
        defaultTaskShouldNotBeFound("scheduleType.equals=" + UPDATED_SCHEDULE_TYPE);
    }

    @Test
    @Transactional
    public void getAllTasksByScheduleTypeIsInShouldWork() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList where scheduleType in DEFAULT_SCHEDULE_TYPE or UPDATED_SCHEDULE_TYPE
        defaultTaskShouldBeFound("scheduleType.in=" + DEFAULT_SCHEDULE_TYPE + "," + UPDATED_SCHEDULE_TYPE);

        // Get all the taskList where scheduleType equals to UPDATED_SCHEDULE_TYPE
        defaultTaskShouldNotBeFound("scheduleType.in=" + UPDATED_SCHEDULE_TYPE);
    }

    @Test
    @Transactional
    public void getAllTasksByScheduleTypeIsNullOrNotNull() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList where scheduleType is not null
        defaultTaskShouldBeFound("scheduleType.specified=true");

        // Get all the taskList where scheduleType is null
        defaultTaskShouldNotBeFound("scheduleType.specified=false");
    }

    @Test
    @Transactional
    public void getAllTasksByDelayIsEqualToSomething() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList where delay equals to DEFAULT_DELAY
        defaultTaskShouldBeFound("delay.equals=" + DEFAULT_DELAY);

        // Get all the taskList where delay equals to UPDATED_DELAY
        defaultTaskShouldNotBeFound("delay.equals=" + UPDATED_DELAY);
    }

    @Test
    @Transactional
    public void getAllTasksByDelayIsInShouldWork() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList where delay in DEFAULT_DELAY or UPDATED_DELAY
        defaultTaskShouldBeFound("delay.in=" + DEFAULT_DELAY + "," + UPDATED_DELAY);

        // Get all the taskList where delay equals to UPDATED_DELAY
        defaultTaskShouldNotBeFound("delay.in=" + UPDATED_DELAY);
    }

    @Test
    @Transactional
    public void getAllTasksByDelayIsNullOrNotNull() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList where delay is not null
        defaultTaskShouldBeFound("delay.specified=true");

        // Get all the taskList where delay is null
        defaultTaskShouldNotBeFound("delay.specified=false");
    }

    @Test
    @Transactional
    public void getAllTasksByDelayIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList where delay greater than or equals to DEFAULT_DELAY
        defaultTaskShouldBeFound("delay.greaterOrEqualThan=" + DEFAULT_DELAY);

        // Get all the taskList where delay greater than or equals to UPDATED_DELAY
        defaultTaskShouldNotBeFound("delay.greaterOrEqualThan=" + UPDATED_DELAY);
    }

    @Test
    @Transactional
    public void getAllTasksByDelayIsLessThanSomething() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList where delay less than or equals to DEFAULT_DELAY
        defaultTaskShouldNotBeFound("delay.lessThan=" + DEFAULT_DELAY);

        // Get all the taskList where delay less than or equals to UPDATED_DELAY
        defaultTaskShouldBeFound("delay.lessThan=" + UPDATED_DELAY);
    }


    @Test
    @Transactional
    public void getAllTasksByCronExpressionIsEqualToSomething() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList where cronExpression equals to DEFAULT_CRON_EXPRESSION
        defaultTaskShouldBeFound("cronExpression.equals=" + DEFAULT_CRON_EXPRESSION);

        // Get all the taskList where cronExpression equals to UPDATED_CRON_EXPRESSION
        defaultTaskShouldNotBeFound("cronExpression.equals=" + UPDATED_CRON_EXPRESSION);
    }

    @Test
    @Transactional
    public void getAllTasksByCronExpressionIsInShouldWork() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList where cronExpression in DEFAULT_CRON_EXPRESSION or UPDATED_CRON_EXPRESSION
        defaultTaskShouldBeFound("cronExpression.in=" + DEFAULT_CRON_EXPRESSION + "," + UPDATED_CRON_EXPRESSION);

        // Get all the taskList where cronExpression equals to UPDATED_CRON_EXPRESSION
        defaultTaskShouldNotBeFound("cronExpression.in=" + UPDATED_CRON_EXPRESSION);
    }

    @Test
    @Transactional
    public void getAllTasksByCronExpressionIsNullOrNotNull() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList where cronExpression is not null
        defaultTaskShouldBeFound("cronExpression.specified=true");

        // Get all the taskList where cronExpression is null
        defaultTaskShouldNotBeFound("cronExpression.specified=false");
    }

    @Test
    @Transactional
    public void getAllTasksByChannelTypeIsEqualToSomething() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList where channelType equals to DEFAULT_CHANNEL_TYPE
        defaultTaskShouldBeFound("channelType.equals=" + DEFAULT_CHANNEL_TYPE);

        // Get all the taskList where channelType equals to UPDATED_CHANNEL_TYPE
        defaultTaskShouldNotBeFound("channelType.equals=" + UPDATED_CHANNEL_TYPE);
    }

    @Test
    @Transactional
    public void getAllTasksByChannelTypeIsInShouldWork() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList where channelType in DEFAULT_CHANNEL_TYPE or UPDATED_CHANNEL_TYPE
        defaultTaskShouldBeFound("channelType.in=" + DEFAULT_CHANNEL_TYPE + "," + UPDATED_CHANNEL_TYPE);

        // Get all the taskList where channelType equals to UPDATED_CHANNEL_TYPE
        defaultTaskShouldNotBeFound("channelType.in=" + UPDATED_CHANNEL_TYPE);
    }

    @Test
    @Transactional
    public void getAllTasksByChannelTypeIsNullOrNotNull() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList where channelType is not null
        defaultTaskShouldBeFound("channelType.specified=true");

        // Get all the taskList where channelType is null
        defaultTaskShouldNotBeFound("channelType.specified=false");
    }

    @Test
    @Transactional
    public void getAllTasksByDescriptionIsEqualToSomething() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList where description equals to DEFAULT_DESCRIPTION
        defaultTaskShouldBeFound("description.equals=" + DEFAULT_DESCRIPTION);

        // Get all the taskList where description equals to UPDATED_DESCRIPTION
        defaultTaskShouldNotBeFound("description.equals=" + UPDATED_DESCRIPTION);
    }

    @Test
    @Transactional
    public void getAllTasksByDescriptionIsInShouldWork() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList where description in DEFAULT_DESCRIPTION or UPDATED_DESCRIPTION
        defaultTaskShouldBeFound("description.in=" + DEFAULT_DESCRIPTION + "," + UPDATED_DESCRIPTION);

        // Get all the taskList where description equals to UPDATED_DESCRIPTION
        defaultTaskShouldNotBeFound("description.in=" + UPDATED_DESCRIPTION);
    }

    @Test
    @Transactional
    public void getAllTasksByDescriptionIsNullOrNotNull() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList where description is not null
        defaultTaskShouldBeFound("description.specified=true");

        // Get all the taskList where description is null
        defaultTaskShouldNotBeFound("description.specified=false");
    }

    @Ignore("ignore test for not supported filtering by 'data' field")
    @Test
    @Transactional
    public void getAllTasksByDataIsEqualToSomething() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList where data equals to DEFAULT_DATA
        defaultTaskShouldBeFound("data.equals=" + DEFAULT_DATA);

        // Get all the taskList where data equals to UPDATED_DATA
        defaultTaskShouldNotBeFound("data.equals=" + UPDATED_DATA);
    }

    @Ignore("ignore test for not supported filtering by 'data' field")
    @Test
    @Transactional
    public void getAllTasksByDataIsInShouldWork() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList where data in DEFAULT_DATA or UPDATED_DATA
        defaultTaskShouldBeFound("data.in=" + DEFAULT_DATA + "," + UPDATED_DATA);

        // Get all the taskList where data equals to UPDATED_DATA
        defaultTaskShouldNotBeFound("data.in=" + UPDATED_DATA);
    }

    @Test
    @Transactional
    public void getAllTasksByDataIsNullOrNotNull() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);

        // Get all the taskList where data is not null
        defaultTaskShouldBeFound("data.specified=true");

        // Get all the taskList where data is null
        defaultTaskShouldNotBeFound("data.specified=false");
    }

    /**
     * Executes the search, and checks that the default entity is returned
     */
    private void defaultTaskShouldBeFound(String filter) throws Exception {
        restTaskMockMvc.perform(get("/api/tasks?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(task.getId().intValue())))
            .andExpect(jsonPath("$.[*].key").value(hasItem(DEFAULT_KEY.toString())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME.toString())))
            .andExpect(jsonPath("$.[*].typeKey").value(hasItem(DEFAULT_TYPE_KEY.toString())))
            .andExpect(jsonPath("$.[*].stateKey").value(hasItem(DEFAULT_STATE_KEY.toString())))
            .andExpect(jsonPath("$.[*].createdBy").value(hasItem(DEFAULT_CREATED_BY.toString())))
            .andExpect(jsonPath("$.[*].startDate").value(hasItem(DEFAULT_START_DATE.toString())))
            .andExpect(jsonPath("$.[*].endDate").value(hasItem(DEFAULT_END_DATE.toString())))
            .andExpect(jsonPath("$.[*].scheduleType").value(hasItem(DEFAULT_SCHEDULE_TYPE.toString())))
            .andExpect(jsonPath("$.[*].delay").value(hasItem(DEFAULT_DELAY.intValue())))
            .andExpect(jsonPath("$.[*].cronExpression").value(hasItem(DEFAULT_CRON_EXPRESSION.toString())))
            .andExpect(jsonPath("$.[*].channelType").value(hasItem(DEFAULT_CHANNEL_TYPE.toString())))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION.toString())))
            .andExpect(jsonPath("$.[*].data").value(hasItem(DEFAULT_DATA.toString())));
    }

    /**
     * Executes the search, and checks that the default entity is not returned
     */
    private void defaultTaskShouldNotBeFound(String filter) throws Exception {
        restTaskMockMvc.perform(get("/api/tasks?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());
    }


    @Test
    @Transactional
    public void getNonExistingTask() throws Exception {
        // Get the task
        restTaskMockMvc.perform(get("/api/tasks/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateTask() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);
        int databaseSizeBeforeUpdate = taskRepository.findAll().size();

        // Update the task
        Task updatedTask = taskRepository.findById(task.getId())
            .orElseThrow(() -> new IllegalArgumentException("Entity not found"));
        // Disconnect from session so that the updates on updatedTask are not directly saved in db
        em.detach(updatedTask);
        updatedTask
            .key(UPDATED_KEY)
            .name(UPDATED_NAME)
            .typeKey(UPDATED_TYPE_KEY)
            .stateKey(UPDATED_STATE_KEY)
            .createdBy(UPDATED_CREATED_BY)
            .startDate(UPDATED_START_DATE)
            .endDate(UPDATED_END_DATE)
            .scheduleType(UPDATED_SCHEDULE_TYPE)
            .delay(UPDATED_DELAY)
            .cronExpression(UPDATED_CRON_EXPRESSION)
            .channelType(UPDATED_CHANNEL_TYPE)
            .description(UPDATED_DESCRIPTION)
            .data(UPDATED_DATA);
        TaskDTO taskDTO = taskMapper.toDto(updatedTask);

        restTaskMockMvc.perform(put("/api/tasks")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(taskDTO)))
            .andExpect(status().isOk());

        // Validate the Task in the database
        List<Task> taskList = taskRepository.findAll();
        assertThat(taskList).hasSize(databaseSizeBeforeUpdate);
        Task testTask = taskList.get(taskList.size() - 1);
        assertThat(testTask.getKey()).isEqualTo(UPDATED_KEY);
        assertThat(testTask.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testTask.getTypeKey()).isEqualTo(UPDATED_TYPE_KEY);
        assertThat(testTask.getStateKey()).isEqualTo(UPDATED_STATE_KEY);
        assertThat(testTask.getCreatedBy()).isEqualTo(UPDATED_CREATED_BY);
        assertThat(testTask.getStartDate()).isEqualTo(UPDATED_START_DATE);
        assertThat(testTask.getEndDate()).isEqualTo(UPDATED_END_DATE);
        assertThat(testTask.getScheduleType()).isEqualTo(UPDATED_SCHEDULE_TYPE);
        assertThat(testTask.getDelay()).isEqualTo(UPDATED_DELAY);
        assertThat(testTask.getCronExpression()).isEqualTo(UPDATED_CRON_EXPRESSION);
        assertThat(testTask.getChannelType()).isEqualTo(UPDATED_CHANNEL_TYPE);
        assertThat(testTask.getDescription()).isEqualTo(UPDATED_DESCRIPTION);
        assertThat(testTask.getData()).isEqualTo(UPDATED_DATA);
    }

    @Test
    @Transactional
    public void updateNonExistingTask() throws Exception {
        int databaseSizeBeforeUpdate = taskRepository.findAll().size();

        // Create the Task
        TaskDTO taskDTO = taskMapper.toDto(task);

        // If the entity doesn't have an ID, it will be created instead of just being updated
        restTaskMockMvc.perform(put("/api/tasks")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(taskDTO)))
            .andExpect(status().isCreated());

        // Validate the Task in the database
        List<Task> taskList = taskRepository.findAll();
        assertThat(taskList).hasSize(databaseSizeBeforeUpdate + 1);
    }

    @Test
    @Transactional
    public void deleteTask() throws Exception {
        // Initialize the database
        taskRepository.saveAndFlush(task);
        int databaseSizeBeforeDelete = taskRepository.findAll().size();

        // Get the task
        restTaskMockMvc.perform(delete("/api/tasks/{id}", task.getId())
                .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<Task> taskList = taskRepository.findAll();
        assertThat(taskList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Task.class);
        Task task1 = new Task();
        task1.setId(1L);
        Task task2 = new Task();
        task2.setId(task1.getId());
        assertThat(task1).isEqualTo(task2);
        task2.setId(2L);
        assertThat(task1).isNotEqualTo(task2);
        task1.setId(null);
        assertThat(task1).isNotEqualTo(task2);
    }

    @Test
    @Transactional
    public void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(TaskDTO.class);
        TaskDTO taskDTO1 = new TaskDTO();
        taskDTO1.setId(1L);
        TaskDTO taskDTO2 = new TaskDTO();
        assertThat(taskDTO1).isNotEqualTo(taskDTO2);
        taskDTO2.setId(taskDTO1.getId());
        assertThat(taskDTO1).isEqualTo(taskDTO2);
        taskDTO2.setId(2L);
        assertThat(taskDTO1).isNotEqualTo(taskDTO2);
        taskDTO1.setId(null);
        assertThat(taskDTO1).isNotEqualTo(taskDTO2);
    }

    @Test
    @Transactional
    public void testEntityFromId() {
        assertThat(taskMapper.fromId(42L).getId()).isEqualTo(42);
        assertThat(taskMapper.fromId(null)).isNull();
    }
}
