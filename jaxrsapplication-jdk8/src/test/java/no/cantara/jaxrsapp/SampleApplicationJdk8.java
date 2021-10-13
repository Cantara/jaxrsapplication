package no.cantara.jaxrsapp;

import no.cantara.config.ApplicationProperties;

public class SampleApplicationJdk8 extends AbstractJaxRsServletApplication<SampleApplicationJdk8> {
    public SampleApplicationJdk8(ApplicationProperties config) {
        super("sample-application-jdk8", config);
    }

    @Override
    public SampleApplicationJdk8 init() {
        return this;
    }
}
