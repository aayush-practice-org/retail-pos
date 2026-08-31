package io.aygh.tenant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates and migrates the Postgres schema behind a tenant.
 * <p>
 * The application's own Flyway run only ever touches {@code public}
 * ({@code spring.flyway.locations}); tenant schemas are migrated from here with
 * a throwaway Flyway instance per schema, each keeping its own history table
 * inside that schema. That is what lets a mart registered today start at the
 * first tenant migration and catch up to the rest on its own, instead of
 * inheriting a history it never ran.
 * <p>
 * Everything here is idempotent, so a failed provisioning can simply be run
 * again: {@code CREATE SCHEMA IF NOT EXISTS} is a no-op the second time, and
 * Flyway applies only what the schema is missing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantSchemaService {

    private static final String TENANT_MIGRATIONS = "classpath:db/migration/tenant";

    private final DataSource dataSource;

    /**
     * Brings the schema for {@code slug} into existence and up to date.
     *
     * @throws TenantProvisioningException if the schema could not be created or migrated
     */
    public void provision(String slug) {
        TenantSlug.requireValid(slug);
        log.info("Provisioning tenant schema '{}'", slug);

        createSchema(slug);
        migrate(slug);

        log.info("Tenant schema '{}' is ready", slug);
    }

    /**
     * Applies any tenant migrations the schema has not seen yet. Used on deploy to
     * roll a new migration out across every existing mart.
     */
    public void migrate(String slug) {
        TenantSlug.requireValid(slug);
        try {
            Flyway.configure()
                    .dataSource(dataSource)
                    .schemas(slug)
                    .defaultSchema(slug)
                    .locations(TENANT_MIGRATIONS)
                    .createSchemas(true)
                    .baselineOnMigrate(false)
                    .outOfOrder(true)
                    .load()
                    .migrate();
        } catch (RuntimeException e) {
            throw new TenantProvisioningException("Failed to migrate tenant schema '" + slug + "'", e);
        }
    }

    /**
     * Migrates every schema given and returns the ones that failed.
     * <p>
     * One tenant's broken schema must not stop the rest from being brought up to
     * date, so each is attempted independently and the failures are collected
     * rather than thrown.
     */
    public List<String> migrateAll(List<String> slugs) {
        List<String> failed = new ArrayList<>();
        for (String slug : slugs) {
            try {
                migrate(slug);
            } catch (RuntimeException e) {
                log.error("Tenant migration failed for schema '{}'", slug, e);
                failed.add(slug);
            }
        }
        return List.copyOf(failed);
    }

    public boolean schemaExists(String slug) {
        TenantSlug.requireValid(slug);
        try (Connection connection = dataSource.getConnection();
             var statement = connection.prepareStatement(
                     "SELECT 1 FROM information_schema.schemata WHERE schema_name = ?")) {
            statement.setString(1, slug);
            try (var rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new TenantProvisioningException("Could not check whether schema '" + slug + "' exists", e);
        }
    }

    /**
     * The slug is interpolated rather than bound because an identifier cannot be a
     * bind parameter. {@link TenantSlug#requireValid} above is what makes that
     * safe — the value is already known to be nothing but lowercase letters,
     * digits and underscores.
     */
    private void createSchema(String slug) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA IF NOT EXISTS \"" + slug + "\"");
        } catch (SQLException e) {
            throw new TenantProvisioningException("Failed to create tenant schema '" + slug + "'", e);
        }
    }
}
