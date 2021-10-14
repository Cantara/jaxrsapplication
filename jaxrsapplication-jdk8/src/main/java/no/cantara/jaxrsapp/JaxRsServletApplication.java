package no.cantara.jaxrsapp;

import java.util.function.Supplier;

public interface JaxRsServletApplication<A extends JaxRsServletApplication> extends JaxRsRegistry {

    A init();

    A override(Class<?> clazz, Supplier<Object> init);

    A start();

    A stop();

    int getBoundPort();
}
