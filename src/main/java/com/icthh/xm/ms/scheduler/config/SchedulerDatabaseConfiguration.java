package com.icthh.xm.ms.scheduler.config;

import com.icthh.xm.commons.migration.db.config.DatabaseConfiguration;
import com.icthh.xm.commons.migration.db.tenant.SchemaResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.jpa.autoconfigure.JpaProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Slf4j
@Configuration
@EnableJpaRepositories(value = {"com.icthh.xm.ms.scheduler.repository"})
public class SchedulerDatabaseConfiguration extends DatabaseConfiguration {

    private static final String JPA_PACKAGES = "com.icthh.xm.ms.scheduler.domain";

    private final Environment env;

    public SchedulerDatabaseConfiguration(Environment env,
                                   JpaProperties jpaProperties,
                                   SchemaResolver schemaResolver) {
        super(env, jpaProperties, schemaResolver);
        this.env = env;
    }

    @Override
    public String getJpaPackages() {
        return JPA_PACKAGES;
    }

}
