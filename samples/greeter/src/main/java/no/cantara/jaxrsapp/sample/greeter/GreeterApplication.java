package no.cantara.jaxrsapp.sample.greeter;

import no.cantara.config.ApplicationProperties;
import no.cantara.jaxrsapp.AbstractJaxRsServletApplication;
import no.cantara.jaxrsapp.health.HealthProbe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GreeterApplication extends AbstractJaxRsServletApplication<GreeterApplication> {

    private static final Logger log = LoggerFactory.getLogger(GreeterApplication.class);

    public static void main(String[] args) {
        ApplicationProperties config = ApplicationProperties.builder()
                .classpathPropertiesFile("greeter/application.properties")
                .filesystemPropertiesFile("application.properties")
                .filesystemPropertiesFile("local_override.properties")
                .filesystemPropertiesFile("application-greeter.properties")
                .enableSystemProperties()
                .enableEnvironmentVariables()
                .build();
        new GreeterApplication(config).init().start();
    }

    public GreeterApplication(ApplicationProperties config) {
        super("greeter", config);
    }

    @Override
    public GreeterApplication init() {
        initSecurity();
        init(RandomizerClient.class, this::createHttpRandomizer);
        GreetingResource greetingResource = initAndRegisterJaxRsWsComponent(GreetingResource.class, this::createGreetingResource);
        initHealth(new HealthProbe("greeting.request.count", greetingResource::getRequestCount));
        return this;
    }

    private HttpRandomizerClient createHttpRandomizer() {
        String randomizerHost = config.get("randomizer.host");
        int randomizerPort = config.asInt("randomizer.port");
        return new HttpRandomizerClient("http://" + randomizerHost + ":" + randomizerPort + "/randomizer");
    }

    private GreetingResource createGreetingResource() {
        RandomizerClient randomizerClient = get(RandomizerClient.class);
        return new GreetingResource(randomizerClient);
    }
}