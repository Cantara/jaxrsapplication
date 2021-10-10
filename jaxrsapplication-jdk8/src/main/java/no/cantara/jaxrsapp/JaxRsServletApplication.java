package no.cantara.jaxrsapp;

import java.util.function.Supplier;

public interface JaxRsServletApplication<A extends JaxRsServletApplication> {

    A init();

    A override(Class<?> clazz, Supplier<Object> init);

    A put(Class<?> clazz, Object instance);

    <T> T get(Class<T> clazz);

    A start();

    A stop();

    int getBoundPort();
}
