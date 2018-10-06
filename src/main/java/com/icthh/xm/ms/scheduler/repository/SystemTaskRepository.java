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

import static org.springframework.util.CollectionUtils.isEmpty;

@Slf4j
@Component
public class ConfigTaskRepository implements RefreshableConfiguration {

    private static final String TENANT_NAME = "tenantName";

    private AntPathMatcher matcher = new AntPathMatcher();
    private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    @Getter
    private ConcurrentHashMap<String, Map<String, TaskDTO>> configTasks = new ConcurrentHashMap();

    private ApplicationProperties applicationProperties;

    public ConfigTaskRepository(ApplicationProperties applicationProperties) {
        log.info("Init of ConfigTaskRepository");
        this.applicationProperties = applicationProperties;
    }

    @Override
    public void onRefresh(String updatedKey, String config) {
        String pathPattern = applicationProperties.getTaskPathPattern();
        try {
            String tenant = matcher.extractUriTemplateVariables(pathPattern, updatedKey).get(TENANT_NAME).toLowerCase();
            if (StringUtils.isBlank(config)) {
                configTasks.remove(tenant);
                log.info("Tasks for tenant '{}' were removed", tenant);
            } else {
                TasksSpec spec = mapper.readValue(config, TasksSpec.class);
                configTasks.put(tenant, toTypeSpecsMap(spec));
                log.info("Tasks for tenant '{}' were updated", tenant);
            }
        } catch (Exception e) {
            log.error("Error read xm specification from path " + updatedKey, e);
        }
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        String taskPathPattern = applicationProperties.getTaskPathPattern();
        return matcher.match(taskPathPattern, updatedKey);
    }

    @Override
    public void onInit(String key, String config) {
        if (isListeningConfiguration(key)) {
            onRefresh(key, config);
        }
    }

    private Map<String, TaskDTO> toTypeSpecsMap(TasksSpec spec) {
        List<TaskDTO> tasks = spec.getTasks();
        if (isEmpty(tasks)) {
            return Collections.emptyMap();
        } else {
            // Convert List<TaskDTO> to Map<key, TaskDTO>
            Map<String, TaskDTO> result = tasks.stream()
                .collect(Collectors.toMap(TaskDTO::getKey, Function.identity(),
                    (u, v) -> {
                        throw new IllegalStateException(String.format("Duplicate key %s", u));
                    }, HashMap::new));

            return result;
        }
    }
}
