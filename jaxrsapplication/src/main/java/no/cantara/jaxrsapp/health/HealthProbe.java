package no.cantara.jaxrsapp.health;

import java.util.function.Supplier;

public class HealthProbe {
    final String key;
    final Supplier<Object> probe;

    public HealthProbe(String key, Supplier<Object> probe) {
        this.key = key;
        this.probe = probe;
    }

    public String getKey() {
        return key;
    }

    public Supplier<Object> getProbe() {
        return probe;
    }
}
