package no.cantara.jaxrsapp.test;

import no.cantara.jaxrsapp.JaxRsRegistry;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class MockRegistry implements Iterable<Class<?>>, JaxRsRegistry {

    private final AtomicReference<JaxRsRegistry> registry = new AtomicReference<>();
    private final Map<Class<?>, Supplier<?>> factoryByType = new ConcurrentHashMap<>();

    public MockRegistry() {
        registry.set(new JaxRsRegistry() {
            @Override
            public <T> JaxRsRegistry put(Class<T> clazz, T instance) {
                return this;
            }

            @Override
            public <T> T get(Class<T> clazz) {
                return null;
            }
        });
    }

    public MockRegistry withJaxRsRegistry(JaxRsRegistry jaxRsRegistry) {
        this.registry.set(jaxRsRegistry);
        return this;
    }

    public <T> MockRegistry addFactory(Supplier<T> factory) {
        factoryByType.put(factory.getClass(), factory);
        return this;
    }

    public <T> MockRegistry addFactory(Class<T> clazz, Supplier<T> factory) {
        factoryByType.put(clazz, factory);
        return this;
    }

    public <T> Supplier<T> getFactory(Class<T> clazz) {
        return (Supplier<T>) factoryByType.get(clazz);
    }

    @Override
    public Iterator<Class<?>> iterator() {
        return factoryByType.keySet().iterator();
    }

    @Override
    public <T> JaxRsRegistry put(Class<T> clazz, T instance) {
        return registry.get().put(clazz, instance);
    }

    @Override
    public <T> T get(Class<T> clazz) {
        return registry.get().get(clazz);
    }
}
