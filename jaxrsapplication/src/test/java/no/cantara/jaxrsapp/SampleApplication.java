package no.cantara.jaxrsapp;

import no.cantara.config.ApplicationProperties;

public class SampleApplication extends AbstractJaxRsServletApplication<SampleApplication> {
    public SampleApplication(ApplicationProperties config) {
        super("test-application", config);
    }

    @Override
    public void doInit() {
    }
}
