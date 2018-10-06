package com.icthh.xm.ms.scheduler.repository;

import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.ms.scheduler.TaskTestUtil;
import com.icthh.xm.ms.scheduler.config.ApplicationProperties;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static com.icthh.xm.commons.tenant.TenantContextUtils.getRequiredTenantKeyValue;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SystemTaskRepositoryTest {

    private static final String PATH_PATTERN = "/config/tenants/{tenantName}/scheduler/tasks.yml";
    private static final String TEST_YAML = "config/tasks/tasks.yml";

    @Mock
    private TenantContextHolder tenantContextHolder;
    @Mock
    private TenantContext tenantContext;

    private SystemTaskRepository systemTaskRepository;
    private String key;
    private String config;

    @Before
    public void init() throws IOException {
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf(TaskTestUtil.TEST_TENANT)));
        when(tenantContextHolder.getContext()).thenReturn(tenantContext);

        ApplicationProperties applicationProperties = new ApplicationProperties();
        applicationProperties.getScheduler().setTaskPathPattern(PATH_PATTERN);

        systemTaskRepository = new SystemTaskRepository(applicationProperties);
        String tenantName = getRequiredTenantKeyValue(tenantContextHolder);
        InputStream cfgInputStream = new ClassPathResource(TEST_YAML).getInputStream();
        config = IOUtils.toString(cfgInputStream, UTF_8);
        key = applicationProperties.getScheduler().getTaskPathPattern().replace("{tenantName}", tenantName);
    }

    @Test
    public void onRefresh() {
        systemTaskRepository.onRefresh(key, config);
        assertThat(systemTaskRepository.getConfigTasks().size()).isEqualTo(1);
        assertThat(systemTaskRepository.getConfigTasks().get(getRequiredTenantKeyValue(tenantContextHolder)).size()).isEqualTo(2);
        assertThat(systemTaskRepository.getConfigTasks().get(getRequiredTenantKeyValue(tenantContextHolder)).get("task-1").getData()).contains("\"subkey\": \"test subkey\"");
        systemTaskRepository.onRefresh(key, null);
        assertThat(systemTaskRepository.getConfigTasks().size()).isEqualTo(0);
    }
}
