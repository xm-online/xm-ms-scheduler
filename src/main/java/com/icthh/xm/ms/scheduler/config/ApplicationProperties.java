package com.icthh.xm.ms.scheduler.config;

import java.util.Collections;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties specific to Scheduler.
 *
 * Properties are configured in the application.yml file.
 * See {@link io.github.jhipster.config.JHipsterProperties} for a good example.
 */
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
@Getter
@Setter
public class ApplicationProperties {

    private final Retry retry = new Retry();
    private final Scheduler scheduler = new Scheduler();

    private List<String> tenantIgnoredPathList = Collections.emptyList();
    // TODO - why do we need this property?
    private boolean streamBindingEnabled = true;
    private boolean kafkaEnabled;
    private String kafkaSystemTopic;
    private String kafkaSystemQueue;
    private String dbSchemaSuffix;
    private String h2Port = "9099";

    private KafkaMetric kafkaMetric;

    @Getter
    @Setter
    public static class Scheduler {
        private int threadPoolSize = 5;
        private String taskPathPattern;
    }

    // TODO - do we need this properties?
    @Getter
    @Setter
    private static class Retry {

        private int maxAttempts;
        private long delay;
        private int multiplier;
    }

    @Getter
    @Setter
    public static class KafkaMetric {
        private boolean enabled;
        private int connectionTimeoutTopic;
        List<String> metricTopics;
    }
}
