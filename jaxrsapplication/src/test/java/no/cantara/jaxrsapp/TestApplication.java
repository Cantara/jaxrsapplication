package no.cantara.jaxrsapp;

import no.cantara.config.ApplicationProperties;

public class TestApplication extends AbstractJaxRsServletApplication<TestApplication> {
    public TestApplication(ApplicationProperties config) {
        super(config);
    }

    @Override
    public TestApplication init() {
        return this;
    }
}
