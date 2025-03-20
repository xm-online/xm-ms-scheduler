package com.icthh.xm.ms.scheduler.repository;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.internal.DefaultTenantContextHolder;
import com.icthh.xm.ms.scheduler.AbstractSpringBootTest;
import com.icthh.xm.ms.scheduler.manager.SchedulingManager;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

import static com.icthh.xm.commons.tenant.TenantContextUtils.getRequiredTenantKeyValue;
import static com.icthh.xm.ms.scheduler.TaskTestUtil.TEST_TENANT;
import static com.icthh.xm.ms.scheduler.repository.SystemTaskRepository.FILE_PATTERN;
import static com.icthh.xm.ms.scheduler.repository.SystemTaskRepository.FOLDER_PATTERN;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class SystemTaskRepositoryTest extends AbstractSpringBootTest {

    private static final String TEST_YAML = "config/tasks/tasks.yml";
    private static final String TEST_ADDITIONAL_YAML = "config/tasks/additional-tasks.yml";
    private static final String TEST_YAML_UPDATED = "config/tasks/tasks-updated.yml";

    private TenantContextHolder tenantContextHolder;

    @Autowired
    private SystemTaskRepository systemTaskRepository;

    @Autowired
    private SchedulingManager schedulingManager;

    private String key;
    private String additionalKey;
    private String config;
    private String additionalConfig;
    private String configUpdated;

    @Before
    @SneakyThrows
    public void init() {

        tenantContextHolder = new DefaultTenantContextHolder();
        TenantContextUtils.setTenant(tenantContextHolder, TEST_TENANT);

        config = readConfig(TEST_YAML);
        additionalConfig = readConfig(TEST_ADDITIONAL_YAML);
        configUpdated = readConfig(TEST_YAML_UPDATED);

        String tenantName = getRequiredTenantKeyValue(tenantContextHolder);
        key = FILE_PATTERN.replace("{tenantName}", tenantName);
        additionalKey = FOLDER_PATTERN.replace("{tenantName}", tenantName).replace("/*.yml", "/additional-tasks.yml");

    }

    private String readConfig(String configPath) throws IOException {
        InputStream cfgInputStream = new ClassPathResource(configPath).getInputStream();
        return IOUtils.toString(cfgInputStream, UTF_8);
    }

    @Test
    public void onRefresh() {
        systemTaskRepository.onRefresh(key, config);
        systemTaskRepository.onRefresh(additionalKey, additionalConfig);
        assertThat(systemTaskRepository.getConfigTasks().size()).isEqualTo(1);
        assertThat(systemTaskRepository.getConfigTasks()
            .get(getRequiredTenantKeyValue(tenantContextHolder))
            .size()).isEqualTo(3);
        assertThat(systemTaskRepository.getConfigTasks()
            .get(getRequiredTenantKeyValue(tenantContextHolder))
            .get("task-1")
            .getData()).contains("\"subkey\": \"test subkey\"");
        assertThat(systemTaskRepository.getConfigTasks()
            .get(getRequiredTenantKeyValue(tenantContextHolder))
            .get("task-3")
            .getData()).contains("\"subkey\": \"test subkey 3\"");
        systemTaskRepository.onRefresh(key, null);
        assertThat(systemTaskRepository.getConfigTasks().size()).isEqualTo(1);
        systemTaskRepository.onRefresh(additionalKey, null);
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
