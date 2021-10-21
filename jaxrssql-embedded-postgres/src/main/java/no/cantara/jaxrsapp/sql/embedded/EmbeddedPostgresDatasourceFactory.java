package no.cantara.jaxrsapp.sql.embedded;

import no.cantara.config.ApplicationProperties;
import no.cantara.jaxrsapp.sql.JaxRsSqlDatasourceFactory;

public class EmbeddedPostgresDatasourceFactory implements JaxRsSqlDatasourceFactory {
    @Override
    public Class<?> providerClass() {
        return EmbeddedPostgresDatasource.class;
    }

    @Override
    public String alias() {
        return "embedded";
    }

    @Override
    public EmbeddedPostgresDatasource create(ApplicationProperties config) {
        String databaseName = config.get("dataSource.databaseName");
        String user = config.get("dataSource.user");
        String password = config.get("dataSource.password");
        return new EmbeddedPostgresDatasource(databaseName, user, password);
    }
}
