package no.cantara.jaxrsapp.test;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MockRegistry implements Iterable<Class<?>> {

    private final Map<Class<?>, Object> instanceByType = new ConcurrentHashMap<>();

    public <T> MockRegistry add(T instance) {
        instanceByType.put(instance.getClass(), instance);
        return this;
    }

    public <T> MockRegistry add(Class<T> clazz, T instance) {
        instanceByType.put(clazz, instance);
        return this;
    }

    public <T> T get(Class<T> clazz) {
        return (T) instanceByType.get(clazz);
    }

    @Override
    public Iterator<Class<?>> iterator() {
        return instanceByType.keySet().iterator();
    }
}
