package no.cantara.jaxrsapp;

import java.util.function.Supplier;

public interface JaxRsRegistry {

    default <T> JaxRsRegistry put(Class<T> clazz, T instance) {
        return put(clazz.getName(), instance);
    }

    default <T> T get(Class<T> clazz) {
        return (T) get(clazz.getName());
    }

    <T> JaxRsRegistry put(String key, T instance);

    <T> T get(String key);

    <T> T getOrNull(String key);

    default <T> T getOrNull(Class<T> clazz) {
        return getOrNull(clazz.getName());
    }

    default <T> T getOrDefault(String key, T defaultValue) {
        T instance = getOrNull(key);
        if (instance == null) {
            return defaultValue;
        }
        return instance;
    }

    default <T> T getOrDefault(String key, Supplier<T> defaultSupplier) {
        T instance = getOrNull(key);
        if (instance == null) {
            return defaultSupplier.get();
        }
        return instance;
    }
}
