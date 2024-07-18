package com.icthh.xm.ms.scheduler.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.ms.scheduler.config.ApplicationProperties;
import com.icthh.xm.ms.scheduler.domain.spec.TasksSpec;
import com.icthh.xm.ms.scheduler.manager.SchedulingManager;
import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

@Slf4j
@Component
public class SystemTaskRepository implements RefreshableConfiguration {

    private static final String TENANT_NAME = "tenantName";
    public static final String FILE_PATTERN = "/config/tenants/{tenantName}/scheduler/tasks.yml";
    public static final String FOLDER_PATTERN = "/config/tenants/{tenantName}/scheduler/tasks/*.yml";

    private AntPathMatcher matcher = new AntPathMatcher();
    private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    @Getter // tenant -> taskKey -> TaskDTO
    private Map<String, Map<String, TaskDTO>> configTasks = new ConcurrentHashMap<>();
    // tenant -> fileName -> taskKey -> TaskDTO
    private Map<String, Map<String, Map<String, TaskDTO>>> configTasksByFiles = new ConcurrentHashMap<>();

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
        return matcher.match(FILE_PATTERN, updatedKey) || matcher.match(FOLDER_PATTERN, updatedKey);
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
            Map<String, Map<String, TaskDTO>> byFiles = configTasksByFiles.computeIfAbsent(tenant, k -> new ConcurrentHashMap<>());
            if (StringUtils.isBlank(config)) {
                byFiles.remove(updatedKey);
                log.info("Tasks for tenant '{}' were removed: {}", tenant, updatedKey);
            } else {
                TasksSpec spec = mapper.readValue(config, TasksSpec.class);
                byFiles.put(updatedKey, toTypeSpecsMap(spec));
                log.info("Tasks for tenant '{}' were updated: {}", tenant, updatedKey);
            }
            updateConfigTasks(tenant, byFiles);
        } catch (Exception e) {
            log.error("Error read Scheduler specification from path: {}", updatedKey, e);
        }
    }

    private void updateConfigTasks(String tenant, Map<String, Map<String, TaskDTO>> byFiles) {
        Map<String, TaskDTO> tasks = new HashMap<>();
        byFiles.values().forEach(tasks::putAll);
        configTasks.put(tenant, tasks);
        if (MapUtils.isEmpty(configTasks.get(tenant))) {
            configTasks.remove(tenant);
        }
    }

    private String extractTenant(final String updatedKey) {
        String pathPattern = getPathPattern(updatedKey);
        return matcher.extractUriTemplateVariables(pathPattern, updatedKey).get(TENANT_NAME);
    }

    private String getPathPattern(String updatedKey) {
        if (matcher.match(FILE_PATTERN, updatedKey)) {
            return FILE_PATTERN;
        } else if (matcher.match(FOLDER_PATTERN, updatedKey)) {
            return FOLDER_PATTERN;
        } else {
            throw new IllegalStateException("Unsupported path: " + updatedKey);
        }
    }
}
