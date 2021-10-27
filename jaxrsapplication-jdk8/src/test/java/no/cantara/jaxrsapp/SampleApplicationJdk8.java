package no.cantara.jaxrsapp;

import no.cantara.config.ApplicationProperties;

public class SampleApplicationJdk8 extends AbstractJaxRsServletApplication<SampleApplicationJdk8> {
    public SampleApplicationJdk8(ApplicationProperties config) {
        super("sample-application-jdk8", config);
    }

    @Override
    protected void doInit() {
        initMetrics();
        initJerseyMetrics();
        initJettyMetrics();
        initJvmMetrics();
        initSecurity();
        initAdminServlet();
        initVersion("test-version");
        initVisualeHealth();
    }
}
