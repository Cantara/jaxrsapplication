package no.cantara.jaxrsapp;

import no.cantara.config.ApplicationProperties;

import java.util.function.Supplier;

public interface JaxRsServletApplication<A extends JaxRsServletApplication> extends JaxRsRegistry {

    String alias();

    ApplicationProperties config();

    A init();

    boolean isInitialized();

    default A override(Class<?> clazz, Supplier<Object> init) {
        return override(clazz.getName(), init);
    }

    A override(String key, Supplier<Object> init);

    A start();

    A stop();

    int getBoundPort();
}
