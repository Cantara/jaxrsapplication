package no.cantara.jaxrsapp.sql.embedded;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
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

    private static class EmbeddedPostgresSingletonHolder {
        private static final EmbeddedPostgres embeddedPostgres;

        static {
            try {
                embeddedPostgres = EmbeddedPostgres.start();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    final EmbeddedPostgres embeddedPostgres;
    final DataSource dataSource;

    public EmbeddedPostgresDatasource() {
        this.embeddedPostgres = EmbeddedPostgresSingletonHolder.embeddedPostgres;
        this.dataSource = embeddedPostgres.getPostgresDatabase();
    }

    public EmbeddedPostgres getEmbeddedPostgres() {
        return embeddedPostgres;
    }

    public DataSource getDataSource() {
        return embeddedPostgres.getPostgresDatabase();
    }

    @Override
    public String info() {
        return "embedded-postgres-to-be-used-only-for-testing";
    }

    @Override
    public void close() {
        try {
            embeddedPostgres.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
