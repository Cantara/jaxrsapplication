package no.cantara.jaxrsapp.test;

import no.cantara.config.ApplicationProperties;

public interface BeforeCreationLifecycleListener {

    void beforeCreation(ApplicationProperties.Builder builder);

}
