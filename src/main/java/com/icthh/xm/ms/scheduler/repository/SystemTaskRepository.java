package com.icthh.xm.ms.scheduler.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.ms.scheduler.config.ApplicationProperties;
import com.icthh.xm.ms.scheduler.domain.spec.TasksSpec;
import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SystemTaskRepository implements RefreshableConfiguration {

    private static final String TENANT_NAME = "tenantName";

    private AntPathMatcher matcher = new AntPathMatcher();
    private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    @Getter
    private Map<String, Map<String, TaskDTO>> configTasks = new ConcurrentHashMap<>();

    private ApplicationProperties applicationProperties;

    public SystemTaskRepository(ApplicationProperties applicationProperties) {
        log.info("Init of SystemTaskRepository");
        this.applicationProperties = applicationProperties;
    }

    @Override
    public void onRefresh(String updatedKey, String config) {
        String pathPattern = applicationProperties.getScheduler().getTaskPathPattern();
        try {
            String tenant = matcher.extractUriTemplateVariables(pathPattern, updatedKey).get(TENANT_NAME).toLowerCase();
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

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        String taskPathPattern = applicationProperties.getScheduler().getTaskPathPattern();
        return matcher.match(taskPathPattern, updatedKey);
    }

    @Override
    public void onInit(String key, String config) {
        if (isListeningConfiguration(key)) {
            onRefresh(key, config);
        }
    }

    private Map<String, TaskDTO> toTypeSpecsMap(TasksSpec spec) {
        return spec.getTasks().stream().collect(Collectors.toMap(TaskDTO::getKey, Function.identity()));
    }
}
