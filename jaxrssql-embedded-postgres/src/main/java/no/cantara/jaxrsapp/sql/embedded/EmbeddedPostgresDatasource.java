package no.cantara.jaxrsapp.sql.embedded;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import io.zonky.test.db.postgres.embedded.FlywayPreparer;
import no.cantara.jaxrsapp.sql.JaxRsSqlDatasource;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class EmbeddedPostgresDatasource implements JaxRsSqlDatasource {

    final EmbeddedPostgres embeddedPostgres;
    final DataSource dataSource;

    public EmbeddedPostgresDatasource() {
        try {
            this.embeddedPostgres = EmbeddedPostgres.start();
            this.dataSource = embeddedPostgres.getPostgresDatabase();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public EmbeddedPostgres getEmbeddedPostgres() {
        return embeddedPostgres;
    }

    public DataSource getDataSource() {
        return embeddedPostgres.getPostgresDatabase();
    }

    @Override
    public void migrate() {
        FlywayPreparer flywayPreparer = FlywayPreparer.forClasspathLocation("db/migration");
        try {
            flywayPreparer.prepare(embeddedPostgres.getPostgresDatabase());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String info() {
        return "embedded-postgres-to-be-used-only-for-testing";
    }

    @Override
    public void close() {
        /*
         * Do not close anything, as the underlying embedded-postgres might be re-used in another test after this close.
         */
    }

    public void clearTables() {
        DataSource dataSource = getDataSource();
        try (Connection connection = dataSource.getConnection()) {
            String sql = loadResourceContentAsString();
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.executeBatch();
            preparedStatement.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String loadResourceContentAsString() {
        StringBuilder sb = new StringBuilder();
        try (InputStream inputStream = getClass().getResourceAsStream("truncate_all_tables.sql")) {
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return sb.toString();
    }
}
