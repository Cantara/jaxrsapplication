package no.cantara.jaxrsapp.sql.discard;

import no.cantara.config.ApplicationProperties;
import no.cantara.jaxrsapp.sql.JaxRsSqlDatasourceFactory;

public class DiscardingPostgresDatasourceFactory implements JaxRsSqlDatasourceFactory {
    @Override
    public Class<?> providerClass() {
        return DiscardingPostgresDatasource.class;
    }

    @Override
    public String alias() {
        return "discard";
    }

    @Override
    public DiscardingPostgresDatasource create(ApplicationProperties config) {
        return new DiscardingPostgresDatasource();
    }
}
