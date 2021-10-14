package no.cantara.jaxrsapp.sql;

import no.cantara.config.ApplicationProperties;
import no.cantara.config.ProviderLoader;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HikariDatasourceTest {

    @Test
    @Disabled
    public void thatHikariDataSourceWorks() throws SQLException {
        ApplicationProperties config = ApplicationProperties.builder()
                .classpathPropertiesFile("hikari.properties")
                .build();
        JaxRsSqlDatasource jaxRsSqlDatasource = ProviderLoader.configure(config, "hikari", JaxRsSqlDatasourceFactory.class);
        jaxRsSqlDatasource.migrate();
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
    }
}
