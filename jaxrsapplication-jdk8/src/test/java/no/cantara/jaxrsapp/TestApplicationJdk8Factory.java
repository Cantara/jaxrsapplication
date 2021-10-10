package no.cantara.jaxrsapp;

import no.cantara.config.ApplicationProperties;

public class TestApplicationJdk8Factory implements JaxRsServletApplicationFactory<TestApplicationJdk8> {
    @Override
    public Class<?> providerClass() {
        return TestApplicationJdk8.class;
    }

    @Override
    public String alias() {
        return "test-application";
    }

    @Override
    public TestApplicationJdk8 create(ApplicationProperties applicationProperties) {
        return new TestApplicationJdk8(applicationProperties);
    }
}
