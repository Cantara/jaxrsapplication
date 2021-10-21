package no.cantara.jaxrsapp.sql.embedded;

import no.cantara.config.ApplicationProperties;
import no.cantara.config.ProviderLoader;
import no.cantara.jaxrsapp.sql.FlywayMigrationHelper;
import no.cantara.jaxrsapp.sql.JaxRsSqlDatasource;
import no.cantara.jaxrsapp.sql.JaxRsSqlDatasourceFactory;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EmbeddedPostgresDatasourceTest {

    private void createDatabase(ApplicationProperties config) {
        JaxRsSqlDatasource jaxRsSqlDatasource = ProviderLoader.configure(config.subTree("flyway.creation.config"), "embedded", JaxRsSqlDatasourceFactory.class);
        FlywayMigrationHelper flywayMigrationHelper = FlywayMigrationHelper.defaultCreation(
                jaxRsSqlDatasource,
                config.get("flyway.migration.config.dataSource.databaseName"),
                config.get("flyway.migration.config.dataSource.user"),
                config.get("flyway.migration.config.dataSource.password"),
                config.get("database.config.dataSource.databaseName"),
                config.get("database.config.dataSource.user"),
                config.get("database.config.dataSource.password")
        );
        flywayMigrationHelper.upgradeDatabase();
        jaxRsSqlDatasource.close();
    }

    private void migrateDatabase(ApplicationProperties config) {
        JaxRsSqlDatasource jaxRsSqlDatasource = ProviderLoader.configure(config.subTree("flyway.migration.config"), "embedded", JaxRsSqlDatasourceFactory.class);
        FlywayMigrationHelper flywayMigrationHelper = FlywayMigrationHelper.forMigration(
                jaxRsSqlDatasource,
                config.get("database.config.dataSource.user")
        );
        flywayMigrationHelper.upgradeDatabase();
        jaxRsSqlDatasource.close();
    }

    @Test
    public void thatEmbeddedDataSourceWorks() throws SQLException {
        ApplicationProperties config = ApplicationProperties.builder()
                .classpathPropertiesFile("embedded.properties")
                .build();
        createDatabase(config);
        migrateDatabase(config);
        JaxRsSqlDatasource jaxRsSqlDatasource = ProviderLoader.configure(config.subTree("database.config"), "embedded", JaxRsSqlDatasourceFactory.class);
        try {
            DataSource dataSource = jaxRsSqlDatasource.getDataSource();
            String generatedPersonId = UUID.randomUUID().toString();
            try (Connection connection = dataSource.getConnection()) {
                try (PreparedStatement ps = connection.prepareStatement("INSERT INTO Person(person_id, firstname, lastname) VALUES(?,?,?)")) {
                    ps.setString(1, generatedPersonId);
                    ps.setString(2, "John");
                    ps.setString(3, "Smith");
                    int n = ps.executeUpdate();
                    assertEquals(1, n);
                }
                try (PreparedStatement ps = connection.prepareStatement("SELECT person_id, firstname, lastname FROM Person WHERE person_id = ?")) {
                    ps.setString(1, generatedPersonId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String personId = rs.getString(1);
                            String firstname = rs.getString(2);
                            String lastname = rs.getString(3);
                            assertEquals(generatedPersonId, personId);
                            assertEquals("John", firstname);
                            assertEquals("Smith", lastname);
                        }
                    }
                }
            }
        } finally {
            jaxRsSqlDatasource.close();
        }
    }
}
