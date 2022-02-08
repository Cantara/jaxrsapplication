package no.cantara.jaxrsapp.sql.discard;

import no.cantara.config.ApplicationProperties;
import no.cantara.config.ProviderLoader;
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
import static org.junit.jupiter.api.Assertions.assertFalse;

public class DiscardingPostgresDatasourceTest {

    @Test
    public void thatEmbeddedDataSourceWorks() throws SQLException {
        JaxRsSqlDatasource jaxRsSqlDatasource = ProviderLoader.configure(ApplicationProperties.builder().build(), "discard", JaxRsSqlDatasourceFactory.class);
        try {
            DataSource dataSource = jaxRsSqlDatasource.getDataSource();
            String generatedPersonId = UUID.randomUUID().toString();
            try (Connection connection = dataSource.getConnection()) {
                try (PreparedStatement ps = connection.prepareStatement("INSERT INTO Person(person_id, firstname, lastname) VALUES(?,?,?)")) {
                    ps.setString(1, generatedPersonId);
                    ps.setString(2, "John");
                    ps.setString(3, "Smith");
                    int n = ps.executeUpdate();
                    assertEquals(0, n); // we do not expect discarding provider to actually insert anything
                }
                try (PreparedStatement ps = connection.prepareStatement("SELECT person_id, firstname, lastname FROM Person WHERE person_id = ?")) {
                    ps.setString(1, generatedPersonId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertFalse(rs.next()); // no result-set from discarding provider
                    }
                }
            }
        } finally {
            jaxRsSqlDatasource.close();
        }
    }
}
