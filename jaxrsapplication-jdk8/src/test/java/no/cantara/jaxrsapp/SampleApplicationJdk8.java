package no.cantara.jaxrsapp;

import no.cantara.config.ApplicationProperties;

public class SampleApplicationJdk8 extends AbstractJaxRsServletApplication<SampleApplicationJdk8> {
    public SampleApplicationJdk8(String alias, ApplicationProperties config) {
        super(alias, "test-version", config);
    }

    @Override
    protected void doInit() {
        initBuiltinDefaults();
    }
}
