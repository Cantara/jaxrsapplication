package no.cantara.jaxrsapp;

import no.cantara.config.ApplicationProperties;

public class SampleApplication extends AbstractJaxRsServletApplication<SampleApplication> {
    public SampleApplication(String alias, ApplicationProperties config) {
        super(alias, "dev-version-1.2.3", config);
    }

    @Override
    protected void doInit() {
        initBuiltinDefaults();
    }
}
