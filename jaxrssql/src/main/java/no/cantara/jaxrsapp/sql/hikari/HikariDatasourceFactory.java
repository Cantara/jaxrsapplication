package no.cantara.jaxrsapp.sql.hikari;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import no.cantara.config.ApplicationProperties;
import no.cantara.jaxrsapp.sql.JaxRsSqlDatasourceFactory;

import java.util.Properties;

public class HikariDatasourceFactory implements JaxRsSqlDatasourceFactory {
    @Override
    public Class<?> providerClass() {
        return HikariDatasource.class;
    }

    @Override
    public String alias() {
        return "hikari";
    }

    @Override
    public HikariDatasource create(ApplicationProperties ap) {
        Properties props = new Properties();
        props.putAll(ap.map());
        //props.put("dataSource.logWriter", new PrintWriter(System.out));
        HikariConfig config = new HikariConfig(props);
        HikariDataSource hikariDataSource = new HikariDataSource(config);
        HikariDatasource hikariJaxRsSqlDatasource = new HikariDatasource(hikariDataSource);
        return hikariJaxRsSqlDatasource;
    }
}
