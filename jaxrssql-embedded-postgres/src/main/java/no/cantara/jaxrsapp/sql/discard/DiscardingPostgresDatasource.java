package no.cantara.jaxrsapp.sql.discard;

import no.cantara.jaxrsapp.sql.JaxRsSqlDatasource;

import javax.sql.DataSource;

public class DiscardingPostgresDatasource implements JaxRsSqlDatasource {

    private final DiscardingDataSource dataSource;

    public DiscardingPostgresDatasource() {
        dataSource = new DiscardingDataSource();
    }

    public DataSource getDataSource() {
        return new DiscardingDataSource();
    }

    @Override
    public String info() {
        return "discarding-postgres-to-be-used-only-for-testing";
    }

    @Override
    public void close() {
    }
}
