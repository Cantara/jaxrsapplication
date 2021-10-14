package no.cantara.jaxrsapp.sql;

import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultDatabaseMigrationHelper {
    private static final Logger log = LoggerFactory.getLogger(DefaultDatabaseMigrationHelper.class);

    private final Flyway flyway;
    private final String dbUrl;

    private final HikariDataSource hikariDataSource;

    public DefaultDatabaseMigrationHelper(HikariDataSource hikariDataSource) {
        this.hikariDataSource = hikariDataSource;
        this.dbUrl = hikariDataSource.getJdbcUrl();

        flyway = Flyway.configure()
                .baselineOnMigrate(true)
                .locations("db/migration")
                .table("schema_version")
                .dataSource(hikariDataSource).load();

        log.info("Flyway - using driver: {}", hikariDataSource.getDriverClassName());
    }

    public void upgradeDatabase() {
        log.info("Upgrading database with url={} using migration files from {}", dbUrl, flyway.getConfiguration().getLocations());
        try {
            flyway.migrate();
        } catch (FlywayException e) {
            log.error("Database upgrade failed using " + dbUrl, e);
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

    public void closeDatasource() {
        this.hikariDataSource.close();
    }
}