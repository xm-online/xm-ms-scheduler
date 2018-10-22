package com.icthh.xm.ms.scheduler.repository;

import static com.icthh.xm.commons.tenant.TenantContextUtils.getRequiredTenantKeyValue;
import static com.icthh.xm.ms.scheduler.TaskTestUtil.TEST_TENANT;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.internal.DefaultTenantContextHolder;
import com.icthh.xm.ms.scheduler.AbstractSpringContextTest;
import com.icthh.xm.ms.scheduler.config.ApplicationProperties;
import com.icthh.xm.ms.scheduler.manager.SchedulingManager;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.io.InputStream;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SystemTaskRepositoryTest extends AbstractSpringContextTest {

    private static final String TEST_YAML = "config/tasks/tasks.yml";
    private static final String TEST_YAML_UPDATED = "config/tasks/tasks-updated.yml";

    private TenantContextHolder tenantContextHolder;

    @Autowired
    private SystemTaskRepository systemTaskRepository;

    @Autowired
    private SchedulingManager schedulingManager;

    @Autowired
    ApplicationProperties applicationProperties;

    private String key;
    private String config;
    private String configUpdated;

    @Before
    @SneakyThrows
    public void init() {

        tenantContextHolder = new DefaultTenantContextHolder();
        TenantContextUtils.setTenant(tenantContextHolder, TEST_TENANT);

        config = readConfig(TEST_YAML);
        configUpdated = readConfig(TEST_YAML_UPDATED);

        String tenantName = getRequiredTenantKeyValue(tenantContextHolder);
        key = applicationProperties.getScheduler().getTaskPathPattern().replace("{tenantName}", tenantName);

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

        assertThat(schedulingManager.getActiveSystemTaskKeys()).isEmpty();

        systemTaskRepository.onRefresh(key, config);
        assertThat(schedulingManager.getActiveSystemTaskKeys()).containsExactlyInAnyOrder("task-1", "task-2");

        systemTaskRepository.onRefresh(key, configUpdated);
        assertThat(schedulingManager.getActiveSystemTaskKeys()).containsExactlyInAnyOrder("task-1", "task-3");

        systemTaskRepository.onRefresh(key, "");
        assertThat(schedulingManager.getActiveSystemTaskKeys()).isEmpty();

    }

}
