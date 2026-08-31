package io.aygh.tenant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Hands Hibernate a pooled connection already pointed at the right schema.
 * <p>
 * Every tenant shares one Hikari pool and one database; only the connection's
 * {@code search_path} differs, which the PostgreSQL driver sets for us in
 * {@link Connection#setSchema(String)}. The alternative — a pool per tenant —
 * multiplies idle connections by the number of marts, and this installation is
 * expected to hold a lot of small ones.
 * <p>
 * The reset in {@link #releaseConnection} is not optional: the connection goes
 * back into a shared pool, and one returned still pointing at a tenant would
 * serve whoever borrows it next from the wrong schema.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TenantConnectionProvider implements MultiTenantConnectionProvider<String> {

    private final DataSource dataSource;

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
        Connection connection = getAnyConnection();
        try {
            connection.setSchema(tenantIdentifier);
            return connection;
        } catch (SQLException | RuntimeException e) {
            // Returning a connection whose schema was not switched would quietly
            // run the caller's statements against public, so give it back and fail.
            try {
                connection.close();
            } catch (SQLException closeFailure) {
                e.addSuppressed(closeFailure);
            }
            throw e;
        }
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        try {
            connection.setSchema(TenantIdentifier.DEFAULT_SCHEMA);
        } catch (SQLException e) {
            // Better to drop a connection that cannot be reset than to hand a
            // tenant-pointed one back to the pool.
            log.warn("Could not reset schema on a connection leaving tenant '{}'; discarding it", tenantIdentifier, e);
        }
        releaseAnyConnection(connection);
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return MultiTenantConnectionProvider.class.equals(unwrapType) || DataSource.class.equals(unwrapType);
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        if (MultiTenantConnectionProvider.class.equals(unwrapType)) {
            return unwrapType.cast(this);
        }
        if (DataSource.class.equals(unwrapType)) {
            return unwrapType.cast(dataSource);
        }
        throw new UnsupportedOperationException("Cannot unwrap to " + unwrapType);
    }
}
