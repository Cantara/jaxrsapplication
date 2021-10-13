package no.cantara.jaxrsapp.health;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class HealthService implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(HealthService.class);

    /*
     * Thread-safe state
     */
    private final AtomicInteger serviceSequence = new AtomicInteger();
    private final String timeAtStart = Instant.now().toString();
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicReference<String> currentHealthSerialized;
    private final Thread healthUpdateThread;
    private final AtomicBoolean shouldRun = new AtomicBoolean(true);
    private final long updateInterval;
    private final TemporalUnit updateIntervalUnit;
    private final String version;
    private final AtomicLong healthComputeTimeMs = new AtomicLong(-1);
    private final List<HealthProbe> healthProbes = new CopyOnWriteArrayList<>();

    /*
     * State only that is only read and written by the healthUpdateThread, so no need for synchronization
     */
    ObjectNode currentHealth;

    public HealthService(long updateInterval, TemporalUnit updateIntervalUnit) {
        this.updateInterval = updateInterval;
        this.updateIntervalUnit = updateIntervalUnit;
        this.currentHealthSerialized = new AtomicReference<>("{}");
        this.version = readVersion("net.whydah.sso", "Entra-SSOLoginWebApp");
        this.healthUpdateThread = new Thread(this, "health-updater-" + serviceSequence.incrementAndGet());
        this.healthUpdateThread.start();
    }

    public boolean isActivelyUpdatingCurrentHealth() {
        return shouldRun.get() && healthUpdateThread.isAlive();
    }

    public String getCurrentHealthJson() {
        return currentHealthSerialized.get();
    }

    public long getHealthComputeTimeMs() {
        return healthComputeTimeMs.get();
    }

    public void shutdown() {
        shouldRun.set(false);
    }

    @Override
    public void run() {
        try {
            // Initializing
            try {
                currentHealth = mapper.createObjectNode();
                currentHealth.put("Status", "false");
                currentHealth.put("version", version);
                currentHealth.put("running since", timeAtStart);
                currentHealthSerialized.set(currentHealth.toPrettyString());
            } catch (Throwable t) {
                log.warn("While setting health initialization message", t);
            }

            // health-update loop
            while (shouldRun.get()) {
                try {
                    boolean changed = updateHealth(currentHealth);
                    if (changed) {
                        currentHealthSerialized.set(currentHealth.toString());
                    }
                    Thread.sleep(Duration.of(updateInterval, updateIntervalUnit).toMillis());
                } catch (Throwable t) {
                    log.error("While updating health", t);
                    {
                        ObjectNode health = mapper.createObjectNode();
                        health.put("Status", "FAIL");
                        health.put("errorMessage", "Exception while updating health");
                        StringWriter strWriter = new StringWriter();
                        t.printStackTrace(new PrintWriter(strWriter));
                        health.put("errorCause", strWriter.toString());
                        currentHealthSerialized.set(health.toPrettyString());
                    }
                    Thread.sleep(Duration.of(updateInterval, updateIntervalUnit).toMillis());
                }
            }
        } catch (Throwable t) {
            log.error("Update thread died!", t);
            {
                ObjectNode health = mapper.createObjectNode();
                health.put("Status", "FAIL");
                health.put("errorMessage", "Health updater thread died with an unexpected error");
                StringWriter strWriter = new StringWriter();
                t.printStackTrace(new PrintWriter(strWriter));
                health.put("errorCause", strWriter.toString());
                currentHealthSerialized.set(health.toPrettyString());
            }
        }
    }

    public HealthService registerHealthProbe(String key, Supplier<Object> probe) {
        healthProbes.add(new HealthProbe(key, probe));
        return this;
    }

    private boolean updateHealth(ObjectNode health) {
        long start = System.currentTimeMillis();
        boolean changed = false;
        changed |= updateField(health, "Status", () -> "OK");
        for (HealthProbe healthProbe : healthProbes) {
            changed |= updateField(health, healthProbe.key, healthProbe.probe);
        }
        long end = System.currentTimeMillis();
        healthComputeTimeMs.set(end - start);
        return changed;
    }

    private boolean updateField(ObjectNode health, String key, Supplier<Object> valueConsumer) {
        Object value = null;
        try {
            value = valueConsumer.get();
        } catch (Throwable t) {
            log.warn(String.format("Ignoring health field, error while attempting to compute field: '%s'", key), t);
        }
        if (value == null) {
            return updateField(health, key, (String) null);
        }
        if (value instanceof String) {
            return updateField(health, key, (String) value);
        }
        if (value instanceof JsonNode) {
            return updateField(health, key, (JsonNode) value);
        }
        return updateField(health, key, String.valueOf(value));
    }

    private boolean updateField(ObjectNode health, String key, String value) {
        JsonNode field = health.get(key);
        if (value == null) {
            if (field == null) {
                return false;
            }
            health.remove(key);
            return true;
        }
        if (field != null && !field.isNull() && field.isTextual() && field.textValue().equals(value)) {
            return false;
        }
        health.put(key, value);
        return true;
    }

    private boolean updateField(ObjectNode health, String key, JsonNode value) {
        JsonNode field = health.get(key);
        if (value == null) {
            if (field == null) {
                return false;
            }
            health.remove(key);
            return true;
        }
        if (field != null && field.equals(value)) {
            return false;
        }
        health.set(key, value);
        return true;
    }

    private String readVersion(String groupId, String artifactId) {
        String resourcePath = String.format("/META-INF/maven/%s/%s/pom.properties", groupId, artifactId);
        URL mavenVersionResource = HealthService.class.getResource(resourcePath);
        if (mavenVersionResource != null) {
            try {
                Properties mavenProperties = new Properties();
                mavenProperties.load(mavenVersionResource.openStream());
                return mavenProperties.getProperty("version", "missing version info in " + resourcePath);
            } catch (IOException e) {
                log.warn("Problem reading version resource from classpath: ", e);
            }
        }
        return "unknown";
    }
}