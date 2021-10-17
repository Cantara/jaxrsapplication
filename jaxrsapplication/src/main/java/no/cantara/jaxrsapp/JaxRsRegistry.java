package no.cantara.jaxrsapp;

public interface JaxRsRegistry {

    default <T> JaxRsRegistry put(Class<T> clazz, T instance) {
        return put(clazz.getName(), instance);
    }

    default <T> T get(Class<T> clazz) {
        return (T) get(clazz.getName());
    }

    <T> JaxRsRegistry put(String key, T instance);

    <T> T get(String key);
}
