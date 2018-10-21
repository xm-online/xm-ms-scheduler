package com.icthh.xm.ms.scheduler.repository;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.ms.scheduler.AbstractSpringContextTest;
import com.icthh.xm.ms.scheduler.TaskTestUtil;
import com.icthh.xm.ms.scheduler.config.ApplicationProperties;
import com.icthh.xm.ms.scheduler.handler.ScheduledTaskHandler;
import com.icthh.xm.ms.scheduler.manager.SchedulingManager;
import com.icthh.xm.ms.scheduler.service.SystemTaskService;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static com.icthh.xm.commons.tenant.TenantContextUtils.getRequiredTenantKeyValue;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SystemTaskRepositoryTest extends AbstractSpringContextTest {

    private static final String PATH_PATTERN = "/config/tenants/{tenantName}/scheduler/tasks.yml";
    private static final String TEST_YAML = "config/tasks/tasks.yml";
    private static final String TEST_YAML_UPDATED = "config/tasks/tasks-updated.yml";

    @Mock
    private TenantContextHolder tenantContextHolder;
    @Mock
    private TenantContext tenantContext;

    @Autowired
    private SystemTaskService systemTaskService;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    @Autowired
    private ScheduledTaskHandler handler;

    @Autowired
    private TenantListRepository tenantListRepository;

    private Multiset<Long> expiredTasks = HashMultiset.create();
    private Multiset<Long> executedTasks = HashMultiset.create();

    private SystemTaskRepository systemTaskRepository;

    private SchedulingManager schedulingManager;

    private String key;
    private String config;
    private String configUpdated;

    @Before
    @SneakyThrows
    public void init() {
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf(TaskTestUtil.TEST_TENANT)));
        when(tenantContextHolder.getContext()).thenReturn(tenantContext);

        ApplicationProperties applicationProperties = new ApplicationProperties();
        applicationProperties.getScheduler().setTaskPathPattern(PATH_PATTERN);

        systemTaskRepository = new SystemTaskRepository(applicationProperties);

        config = readConfig(TEST_YAML);
        configUpdated = readConfig(TEST_YAML_UPDATED);

        String tenantName = getRequiredTenantKeyValue(tenantContextHolder);
        key = applicationProperties.getScheduler().getTaskPathPattern().replace("{tenantName}", tenantName);

        schedulingManager = new SchedulingManager(tenantContextHolder, taskScheduler, systemTaskService, handler,
                                                  executed -> executedTasks.add(executed.getId()),
                                                  expired -> expiredTasks.add(expired.getId()),
                                                  tenantListRepository);

    }

    private String readConfig(String configPath) throws IOException {
        InputStream cfgInputStream = new ClassPathResource(configPath).getInputStream();
        return IOUtils.toString(cfgInputStream, UTF_8);
    }

    @Test
    public void onRefresh() {
        systemTaskRepository.onRefresh(key, config);
        assertThat(systemTaskRepository.getConfigTasks().size()).isEqualTo(1);
        assertThat(systemTaskRepository.getConfigTasks()
                                       .get(getRequiredTenantKeyValue(tenantContextHolder))
                                       .size()).isEqualTo(2);
        assertThat(systemTaskRepository.getConfigTasks()
                                       .get(getRequiredTenantKeyValue(tenantContextHolder))
                                       .get("task-1")
                                       .getData()).contains("\"subkey\": \"test subkey\"");
        systemTaskRepository.onRefresh(key, null);
        assertThat(systemTaskRepository.getConfigTasks().size()).isEqualTo(0);
    }

    @Test
    public void testUpdateSystemTasksFromConfig() {

//        schedulingManager.init();

        assertThat(schedulingManager.getActiveTaskKeys()).isEmpty();

        systemTaskRepository.onRefresh(key, config);

        System.out.println(schedulingManager.getActiveTaskKeys());

        assertThat(schedulingManager.getActiveTaskKeys()).containsExactlyInAnyOrder("task-1", "task-2");

    }

}
