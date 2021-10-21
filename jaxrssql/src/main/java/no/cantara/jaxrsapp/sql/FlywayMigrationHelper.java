package no.cantara.jaxrsapp.sql;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlywayMigrationHelper {
    private static final Logger log = LoggerFactory.getLogger(FlywayMigrationHelper.class);

    private final Flyway flyway;
    private final String info;

    private final JaxRsSqlDatasource jaxRsSqlDatasource;

    public FlywayMigrationHelper(JaxRsSqlDatasource jaxRsSqlDatasource, String flywayMigrationFolder) {
        this.jaxRsSqlDatasource = jaxRsSqlDatasource;
        this.info = jaxRsSqlDatasource.info();

        flyway = Flyway.configure()
                .baselineOnMigrate(false)
                .locations(flywayMigrationFolder)
                .table("schema_version")
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