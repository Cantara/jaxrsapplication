package no.cantara.jaxrsapp;

import no.cantara.config.ApplicationProperties;
import no.cantara.config.ProviderLoader;
import org.junit.jupiter.api.Test;

public class JaxRsServletApplicationTestJdk8 {

    @Test
    public void thatStackCanBeStartedAndStoppedWithoutIssueJdk8() {
        ApplicationProperties config = ApplicationProperties.builder().testDefaults().build();
        TestApplicationJdk8 application = (TestApplicationJdk8) ProviderLoader.configure(config, "test-application-jdk8", JaxRsServletApplicationFactory.class);
        application.init();
        application.start();
        application.stop();
    }
}
