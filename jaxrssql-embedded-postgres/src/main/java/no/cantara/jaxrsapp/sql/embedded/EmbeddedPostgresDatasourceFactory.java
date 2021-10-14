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
    public EmbeddedPostgresDatasource create(ApplicationProperties applicationProperties) {
        /*
         * Used for testing purposes, so we return the one-and-only singleton.
         */
        return EmbeddedPostgresDatasourceHolder.postgresDatasource;
    }

    private static class EmbeddedPostgresDatasourceHolder {
        private static final EmbeddedPostgresDatasource postgresDatasource;

        static {
            postgresDatasource = new EmbeddedPostgresDatasource();
            postgresDatasource.migrate();
        }
    }
}
