package no.cantara.jaxrsapp.sample.greeter;

import no.cantara.config.ApplicationProperties;
import no.cantara.jaxrsapp.AbstractJaxRsServletApplication;
import no.cantara.jaxrsapp.health.HealthProbe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;

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
        PrintWriter pw = init(PrintWriter.class, this::createAuditTo);
        pw.printf("AUDIT: I am the Greeting application!%n").flush();
        init(GreetingCandidateRepository.class, this::createGreetingCandidateRepository);
        init(RandomizerClient.class, this::createHttpRandomizer);
        GreetingResource greetingResource = initAndRegisterJaxRsWsComponent(GreetingResource.class, this::createGreetingResource);
        initHealth(new HealthProbe("greeting.request.count", greetingResource::getRequestCount));
        return this;
    }

    private PrintWriter createAuditTo() {
        return new PrintWriter(System.err);
    }

    private GreetingCandidateRepository createGreetingCandidateRepository() {
        return () -> List.of(
                        "Hello",
                        "Yo",
                        "Hola",
                        "Hei"
                ).stream()
                .map(GreetingCandidate::new)
                .collect(Collectors.toList());
    }

    private HttpRandomizerClient createHttpRandomizer() {
        String randomizerHost = config.get("randomizer.host");
        int randomizerPort = config.asInt("randomizer.port");
        return new HttpRandomizerClient("http://" + randomizerHost + ":" + randomizerPort + "/randomizer");
    }

    private GreetingResource createGreetingResource() {
        RandomizerClient randomizerClient = get(RandomizerClient.class);
        GreetingCandidateRepository greetingCandidateRepository = get(GreetingCandidateRepository.class);
        return new GreetingResource(greetingCandidateRepository, randomizerClient);
    }
}
