package no.cantara.jaxrsapp.sample.randomizer;

import no.cantara.config.ApplicationProperties;
import no.cantara.config.ProviderLoader;
import no.cantara.jaxrsapp.JaxRsServletApplication;
import no.cantara.jaxrsapp.test.JaxRsServletApplicationLifecycleListener;
import no.cantara.security.authorization.AccessManager;
import no.cantara.security.authorization.AccessManagerFactory;

public class RandomizerApplicationLifecycleListener implements JaxRsServletApplicationLifecycleListener {

    @Override
    public void beforeInit(JaxRsServletApplication application) {
        application.override(AccessManager.class, this::createAccessManager);
    }

    AccessManager createAccessManager() {
        ApplicationProperties authConfig = ApplicationProperties.builder()
                .classpathPropertiesFile("greet/service-authorization.properties")
                .classpathPropertiesFile("greet/authorization.properties")
                .filesystemPropertiesFile("greet/authorization.properties")
                .build();
        AccessManager accessManager = ProviderLoader.configure(authConfig, "default", AccessManagerFactory.class);
        return accessManager;
    }
}
