package no.cantara.jaxrsapp;

import no.cantara.config.ApplicationProperties;
import no.cantara.config.ProviderLoader;
import org.junit.jupiter.api.Test;

public class JaxRsServletApplicationTestJdk8 {

    @Test
    public void thatStackCanBeStartedAndStoppedWithoutIssueJdk8() {
        TestApplicationJdk8 application = (TestApplicationJdk8) ProviderLoader.configure(ApplicationProperties.builder().testDefaults().build(), "test-application", JaxRsServletApplicationFactory.class);
        application.init();
        application.start();
        application.stop();
    }
}
