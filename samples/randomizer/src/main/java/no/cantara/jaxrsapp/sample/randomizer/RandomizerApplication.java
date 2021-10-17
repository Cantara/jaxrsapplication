package no.cantara.jaxrsapp.sample.randomizer;

import no.cantara.config.ApplicationProperties;
import no.cantara.jaxrsapp.AbstractJaxRsServletApplication;
import no.cantara.jaxrsapp.health.HealthProbe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.Random;

public class RandomizerApplication extends AbstractJaxRsServletApplication<RandomizerApplication> {

    private static final Logger log = LoggerFactory.getLogger(RandomizerApplication.class);

    public static void main(String[] args) {
        ApplicationProperties config = ApplicationProperties.builder()
                .classpathPropertiesFile("randomizer/application.properties")
                .filesystemPropertiesFile("application.properties")
                .filesystemPropertiesFile("local_override.properties")
                .filesystemPropertiesFile("application-greeter.properties")
                .enableSystemProperties()
                .enableEnvironmentVariables()
                .build();
        new RandomizerApplication(config).init().start();
    }

    public RandomizerApplication(ApplicationProperties config) {
        super("randomizer", config);
    }

    @Override
    public void doInit() {
        initSecurity();
        PrintWriter pw = init(PrintWriter.class, this::createAuditTo);
        pw.printf("AUDIT: I am the Randomizer application!%n").flush();
        init(Random.class, this::createRandom);
        RandomizerResource randomizerResource = initAndRegisterJaxRsWsComponent(RandomizerResource.class, this::createGreetingResource);
        initHealth(new HealthProbe("randomizer.request.count", randomizerResource::getRequestCount));
    }

    private PrintWriter createAuditTo() {
        return new PrintWriter(System.err);
    }

    private Random createRandom() {
        return new Random(System.currentTimeMillis());
    }

    private RandomizerResource createGreetingResource() {
        Random random = get(Random.class);
        return new RandomizerResource(random);
    }
}
