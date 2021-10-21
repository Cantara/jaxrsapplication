package no.cantara.jaxrsapp.sql.hikari;

import com.zaxxer.hikari.HikariDataSource;
import no.cantara.jaxrsapp.sql.JaxRsSqlDatasource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HikariDatasource implements JaxRsSqlDatasource {
    private static final Logger log = LoggerFactory.getLogger(HikariDatasource.class);

    final HikariDataSource dataSource;

    public HikariDatasource(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public String info() {
        return dataSource.getJdbcUrl();
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
