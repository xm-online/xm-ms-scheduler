package com.icthh.xm.ms.scheduler.config;

import static com.icthh.xm.ms.scheduler.config.Constants.CHANGE_LOG_PATH;

import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.migration.db.XmSpringLiquibase;
import com.icthh.xm.ms.scheduler.util.DatabaseUtil;
import io.github.jhipster.config.JHipsterConstants;
import io.github.jhipster.config.h2.H2ConfigurationHelper;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import liquibase.integration.spring.SpringLiquibase;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Slf4j
@Configuration
@EnableJpaRepositories("com.icthh.xm.ms.scheduler.repository")
@EnableJpaAuditing(auditorAwareRef = "springSecurityAuditorAware")
@EnableTransactionManagement
@AllArgsConstructor
public class DatabaseConfiguration {

    private static final String JPA_PACKAGES = "com.icthh.xm.ms.scheduler.domain";

    private final Environment env;
    private final TenantListRepository tenantListRepository;

    /**
     * Open the TCP port for the H2 database, so it is available remotely.
     *
     * @return the H2 database TCP server
     * @throws SQLException if the server failed to start
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    @Profile(JHipsterConstants.SPRING_PROFILE_DEVELOPMENT)
    public Object h2TCPServer() throws SQLException {
        log.debug("Starting H2 database");
        return H2ConfigurationHelper.createServer();
    }

    /**
     * Liquibase configuration
     *
     * @param dataSource          db data source
     * @param liquibaseProperties configuration properties
     * @return instance of the {@link SpringLiquibase} bin
     */
    @Bean
    public SpringLiquibase liquibase(DataSource dataSource, LiquibaseProperties liquibaseProperties) {
        createSchemas(dataSource);
        SpringLiquibase liquibase = new XmSpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog(CHANGE_LOG_PATH);
        liquibase.setContexts(liquibaseProperties.getContexts());
        liquibase.setDefaultSchema(liquibaseProperties.getDefaultSchema());
        liquibase.setDropFirst(liquibaseProperties.isDropFirst());
        if (env.acceptsProfiles(JHipsterConstants.SPRING_PROFILE_NO_LIQUIBASE)) {
            liquibase.setShouldRun(false);
        } else {
            liquibase.setShouldRun(liquibaseProperties.isEnabled());
            log.debug("Configuring Liquibase");
        }
        return liquibase;
    }

    private void createSchemas(DataSource dataSource) {
        for (String schema : getSchemas()) {
            try {
                DatabaseUtil.createSchema(dataSource, schema);
            } catch (Exception e) {
                log.error("Failed to create schema '{}', error: {}", schema, e.getMessage(), e);
            }
        }
    }

    private List<String> getSchemas() {
        return new ArrayList<>(tenantListRepository.getTenants());
    }

    @Bean
    public Hibernate5Module hibernate5Module() {
        return new Hibernate5Module();
    }

    @Bean
    public JpaVendorAdapter jpaVendorAdapter() {
        return new HibernateJpaVendorAdapter();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
        DataSource dataSource,
        MultiTenantConnectionProvider multiTenantConnectionProviderImpl,
        CurrentTenantIdentifierResolver currentTenantIdentifierResolverImpl) {
        Map<String, Object> properties = new HashMap<>();
        //properties.putAll(jpaProperties.getHibernateProperties(dataSource));
        properties.put(org.hibernate.cfg.Environment.MULTI_TENANT, MultiTenancyStrategy.SCHEMA);
        properties
            .put(org.hibernate.cfg.Environment.MULTI_TENANT_CONNECTION_PROVIDER, multiTenantConnectionProviderImpl);
        properties
            .put(org.hibernate.cfg.Environment.MULTI_TENANT_IDENTIFIER_RESOLVER, currentTenantIdentifierResolverImpl);

        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan(JPA_PACKAGES);
        em.setJpaVendorAdapter(jpaVendorAdapter());
        em.setJpaPropertyMap(properties);
        return em;
    }
}
