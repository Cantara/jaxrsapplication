package no.cantara.security.authorization;

import no.cantara.config.ApplicationProperties;

public class DefaultAccessManagerFactory implements AccessManagerFactory {
    @Override
    public Class<?> providerClass() {
        return DefaultAccessManager.class;
    }

    @Override
    public String alias() {
        return "default";
    }

    @Override
    public DefaultAccessManager create(ApplicationProperties applicationProperties) {
        String actionService = applicationProperties.get("service", "local");
        return new DefaultAccessManager(actionService, applicationProperties);
    }
}
