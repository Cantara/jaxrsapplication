package no.cantara.jaxrsapp.test.greet;

import no.cantara.config.ApplicationProperties;
import no.cantara.jaxrsapp.JaxRsServletApplicationFactory;

public class GreetApplicationFactory implements JaxRsServletApplicationFactory<GreetApplication> {

    @Override
    public Class<?> providerClass() {
        return GreetApplication.class;
    }

    @Override
    public String alias() {
        return "greet";
    }

    @Override
    public GreetApplication create(ApplicationProperties applicationProperties) {
        return new GreetApplication(applicationProperties);
    }
}
