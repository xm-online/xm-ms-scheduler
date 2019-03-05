package com.icthh.xm.ms.scheduler.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.ms.scheduler.config.ApplicationProperties;
import com.icthh.xm.ms.scheduler.domain.spec.TasksSpec;
import com.icthh.xm.ms.scheduler.manager.SchedulingManager;
import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

@Slf4j
@Component
public class SystemTaskRepository implements RefreshableConfiguration {

    private static final String TENANT_NAME = "tenantName";

    private AntPathMatcher matcher = new AntPathMatcher();
    private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    @Getter
    private Map<String, Map<String, TaskDTO>> configTasks = new ConcurrentHashMap<>();

    private final ApplicationProperties applicationProperties;

    private final SchedulingManager schedulingManager;

    // TODO - avoid @Lazy initialization
    public SystemTaskRepository(ApplicationProperties applicationProperties,
                                @Lazy SchedulingManager schedulingManager) {
        log.info("Init of SystemTaskRepository");
        this.applicationProperties = applicationProperties;
        this.schedulingManager = schedulingManager;
    }

    @Override
    public void onRefresh(String updatedKey, String config) {
        refreshConfig(updatedKey, config);
        schedulingManager.mergeSystemTasksFromConfig(extractTenant(updatedKey));
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        String taskPathPattern = applicationProperties.getScheduler().getTaskPathPattern();
        return matcher.match(taskPathPattern, updatedKey);
    }

    @Override
    public void onInit(String key, String config) {
        if (isListeningConfiguration(key)) {
            refreshConfig(key, config);
        }
    }

    private Map<String, TaskDTO> toTypeSpecsMap(TasksSpec spec) {
        return spec.getTasks().stream().collect(Collectors.toMap(TaskDTO::getKey, Function.identity()));
    }

    private void refreshConfig(String updatedKey, String config) {
        try {
            String tenant = extractTenant(updatedKey);
            if (StringUtils.isBlank(config)) {
                configTasks.remove(tenant);
                log.info("Tasks for tenant '{}' were removed: {}", tenant, updatedKey);
            } else {
                TasksSpec spec = mapper.readValue(config, TasksSpec.class);
                configTasks.put(tenant, toTypeSpecsMap(spec));
                log.info("Tasks for tenant '{}' were updated: {}", tenant, updatedKey);
            }
        } catch (Exception e) {
            log.error("Error read Scheduler specification from path: {}", updatedKey, e);
        }
    }

    private String extractTenant(final String updatedKey) {
        String pathPattern = applicationProperties.getScheduler().getTaskPathPattern();
        return matcher.extractUriTemplateVariables(pathPattern, updatedKey).get(TENANT_NAME);
    }
}
