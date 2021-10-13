package no.cantara.jaxrsapp;

import no.cantara.config.ApplicationProperties;
import no.cantara.config.ProviderLoader;
import org.junit.jupiter.api.Test;

public class JaxRsServletApplicationTest {

    @Test
    public void thatStackCanBeStartedAndStoppedWithoutIssue() {
        ApplicationProperties config = ApplicationProperties.builder().testDefaults().build();
        SampleApplication application = (SampleApplication) ProviderLoader.configure(config, "sample-application", JaxRsServletApplicationFactory.class);
        application.init();
        application.start();
        application.stop();
    }
}
