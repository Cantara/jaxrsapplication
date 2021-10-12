package no.cantara.jaxrsapp.sample.greeter;

import no.cantara.config.ApplicationProperties;
import no.cantara.jaxrsapp.JaxRsServletApplicationFactory;

public class GreeterApplicationFactory implements JaxRsServletApplicationFactory<GreeterApplication> {

    @Override
    public Class<?> providerClass() {
        return GreeterApplication.class;
    }

    @Override
    public String alias() {
        return "greeter";
    }

    @Override
    public GreeterApplication create(ApplicationProperties config) {
        return new GreeterApplication(config);
    }
}
