package no.cantara.jaxrsapp.sql;

import javax.sql.DataSource;

public interface JaxRsSqlDatasource {

    /**
     * Get the datasource. This should be a connection-pool configured to use the applications configured postgres database.
     *
     * @return
     */
    DataSource getDataSource();

    /**
     * @return Information about the datasource configuration. Used for debugging and health-info.
     */
    String info();

    /**
     * Close connection-pool and/or data-sources as returned by getDataSource().
     */
    void close();
}
