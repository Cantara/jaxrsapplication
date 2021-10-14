package no.cantara.jaxrsapp;

public interface JaxRsRegistry {

    <T> JaxRsRegistry put(Class<T> clazz, T instance);

    <T> T get(Class<T> clazz);
}
