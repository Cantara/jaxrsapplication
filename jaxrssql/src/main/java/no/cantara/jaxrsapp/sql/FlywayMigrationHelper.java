package no.cantara.jaxrsapp.sql;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

public class FlywayMigrationHelper {
    private static final Logger log = LoggerFactory.getLogger(FlywayMigrationHelper.class);

    private final Flyway flyway;
    private final String info;

    private final JaxRsSqlDatasource jaxRsSqlDatasource;

    public static FlywayMigrationHelper defaultCreation(
            JaxRsSqlDatasource jaxRsSqlDatasource,
            String migrationDatabase, String migrationUser, String migrationPassword,
            String database, String user, String password) {
        return forCreation("db/default-creation", jaxRsSqlDatasource, migrationDatabase, migrationUser, migrationPassword, database, user, password);
    }

    public static FlywayMigrationHelper forCreation(
            String flywayMigrationFolder, JaxRsSqlDatasource jaxRsSqlDatasource,
            String migrationDatabase, String migrationUser, String migrationPassword,
            String database, String user, String password
    ) {
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("migration.database", migrationDatabase);
        placeholders.put("migration.user", migrationUser);
        placeholders.put("migration.password", migrationPassword);
        placeholders.put("app.database", database);
        placeholders.put("app.user", user);
        placeholders.put("app.password", password);
        return new FlywayMigrationHelper(jaxRsSqlDatasource, flywayMigrationFolder, placeholders);
    }

    public static FlywayMigrationHelper forMigration(
            JaxRsSqlDatasource jaxRsSqlDatasource,
            String user
    ) {
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("app.user", user);
        return new FlywayMigrationHelper(jaxRsSqlDatasource, "db/migration", placeholders);
    }

    public FlywayMigrationHelper(JaxRsSqlDatasource jaxRsSqlDatasource, String flywayMigrationFolder, Map<String, String> placeholders) {
        this.jaxRsSqlDatasource = jaxRsSqlDatasource;
        this.info = jaxRsSqlDatasource.info();
        flyway = Flyway.configure()
                .baselineOnMigrate(false)
                .locations(flywayMigrationFolder)
                .table("schema_version")
                .placeholders(placeholders)
                .dataSource(jaxRsSqlDatasource.getDataSource()).load();
    }

    public void upgradeDatabase() {
        log.info("Upgrading database {} using migration files from {}", info, flyway.getConfiguration().getLocations());
        try {
            flyway.migrate();
        } catch (FlywayException e) {
            log.error("Database upgrade failed using " + info, e);
        }
    }

    //used by tests
    public void cleanDatabase() {
        try {
            flyway.clean();
        } catch (FlywayException e) {
            throw new RuntimeException("Database cleaning failed.", e);
        }
    }
}