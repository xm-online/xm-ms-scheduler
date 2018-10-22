package com.icthh.xm.ms.scheduler.config.tenant.hibernate;

import com.icthh.xm.ms.scheduler.config.tenant.SchemaChangeResolver;
import lombok.RequiredArgsConstructor;
import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@RequiredArgsConstructor
@Component
public class MultiTenantConnectionProviderImpl implements MultiTenantConnectionProvider {

    private static final long serialVersionUID = 1L;

    private final transient DataSource dataSource;
    private final SchemaChangeResolver resolver;

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {

        final Connection connection = getAnyConnection();
        try (Statement statement = connection.createStatement()) {
            statement.execute(String.format(resolver.getSchemaSwitchCommand(), tenantIdentifier));
        } catch (SQLException e) {
            throw new HibernateException(
                "Could not alter JDBC connection to specified schema [" + tenantIdentifier + "]", e
            );
        }
        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public boolean isUnwrappableAs(Class unwrapType) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        return null;
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return true;
    }

}
