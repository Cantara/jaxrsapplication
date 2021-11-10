package no.cantara.jaxrsapp.sample.greeter;

import com.codahale.metrics.MetricRegistry;
import no.cantara.config.ApplicationProperties;
import no.cantara.jaxrsapp.AbstractJaxRsServletApplication;
import no.cantara.jaxrsapp.health.HealthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class GreeterApplication extends AbstractJaxRsServletApplication<GreeterApplication> {

    private static final Logger log = LoggerFactory.getLogger(GreeterApplication.class);

    public static void main(String[] args) {
        ApplicationProperties config = new GreeterApplicationFactory()
                .conventions(ApplicationProperties.builder())
                .build();
        new GreeterApplication(config).init().start();
    }

    public GreeterApplication(ApplicationProperties config) {
        super("greeter",
                readMetaInfMavenPomVersion("no.cantara.jaxrsapp", "greeter"),
                config
        );
    }

    @Override
    public void doInit() {
        initBuiltinDefaults();
        init(GreetingCandidateRepository.class, this::createGreetingCandidateRepository);
        init(RandomizerClient.class, this::createHttpRandomizer);
        init(GreetingService.class, this::createGreetingService);
        GreetingResource greetingResource = initAndRegisterJaxRsWsComponent(GreetingResource.class, this::createGreetingResource);
        get(HealthService.class).registerHealthProbe("greeting.request.count", greetingResource::getRequestCount);
    }

    private GreetingCandidateRepository createGreetingCandidateRepository() {
        return new DefaultGreetingCandidateRepository(List.of("Hello", "Yo", "Hola", "Hei"));
    }

    private HttpRandomizerClient createHttpRandomizer() {
        String randomizerHost = config.get("randomizer.host");
        int randomizerPort = config.asInt("randomizer.port");
        return new HttpRandomizerClient("http://" + randomizerHost + ":" + randomizerPort + "/randomizer");
    }

    private GreetingService createGreetingService() {
        RandomizerClient randomizerClient = get(RandomizerClient.class);
        GreetingCandidateRepository greetingCandidateRepository = get(GreetingCandidateRepository.class);
        MetricRegistry metricRegistry = get(MetricRegistry.class);
        return new GreetingService(metricRegistry, greetingCandidateRepository, randomizerClient);
    }

    private GreetingResource createGreetingResource() {
        GreetingService greetingService = get(GreetingService.class);
        return new GreetingResource(greetingService);
    }
}
