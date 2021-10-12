package no.cantara.jaxrsapp;

import no.cantara.config.ApplicationProperties;

public class TestApplicationJdk8 extends AbstractJaxRsServletApplication<TestApplicationJdk8> {
    public TestApplicationJdk8(ApplicationProperties config) {
        super("test-application-jdk8", config);
    }

    @Override
    public TestApplicationJdk8 init() {
        return this;
    }
}
