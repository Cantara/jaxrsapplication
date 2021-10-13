package no.cantara.jaxrsapp;

import no.cantara.config.ApplicationProperties;
import no.cantara.config.ProviderLoader;
import org.junit.jupiter.api.Test;

public class JaxRsServletApplicationJdk8Test {

    @Test
    public void thatStackCanBeStartedAndStoppedWithoutIssueJdk8() {
        ApplicationProperties config = ApplicationProperties.builder().testDefaults().build();
        SampleApplicationJdk8 application = (SampleApplicationJdk8) ProviderLoader.configure(config, "sample-application-jdk8", JaxRsServletApplicationFactory.class);
        application.init();
        application.start();
        application.stop();
    }
}
