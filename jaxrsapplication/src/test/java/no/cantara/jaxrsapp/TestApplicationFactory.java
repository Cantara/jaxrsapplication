package no.cantara.jaxrsapp;

import no.cantara.config.ApplicationProperties;

public class TestApplicationFactory implements JaxRsServletApplicationFactory<TestApplication> {
    @Override
    public Class<?> providerClass() {
        return TestApplication.class;
    }

    @Override
    public String alias() {
        return "test-application";
    }

    @Override
    public TestApplication create(ApplicationProperties applicationProperties) {
        return new TestApplication(applicationProperties);
    }
}
