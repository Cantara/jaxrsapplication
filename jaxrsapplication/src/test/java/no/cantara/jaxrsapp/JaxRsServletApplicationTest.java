package no.cantara.jaxrsapp;

import no.cantara.config.ApplicationProperties;
import no.cantara.config.ProviderLoader;
import org.junit.jupiter.api.Test;

public class JaxRsServletApplicationTest {

    @Test
    public void thatStackCanBeStartedAndStoppedWithoutIssue() {
        TestApplication application = (TestApplication) ProviderLoader.configure(ApplicationProperties.builder().testDefaults().build(), "test-application", JaxRsServletApplicationFactory.class);
        application.init();
        application.start();
        application.stop();
    }
}
