package no.cantara.jaxrsapp.sample.greeter;

import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.servlet.DispatcherType;
import no.cantara.config.ApplicationProperties;
import no.cantara.jaxrsapp.AbstractJaxRsServletApplication;
import no.cantara.jaxrsapp.health.HealthProbe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.EnumSet;
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
    public void doInit() {
        initSecurity();
        initAndAddServletFilter(CORSServletFilter.class, CORSServletFilter::new, "/*", EnumSet.allOf(DispatcherType.class));
        initAndRegisterJaxRsWsComponent(JaxRsOpenApiResource.class.getName(), this::createOpenApiResource);
        PrintWriter pw = init(PrintWriter.class, this::createAuditTo);
        pw.printf("AUDIT: I am the Greeting application!%n").flush();
        init(GreetingCandidateRepository.class, this::createGreetingCandidateRepository);
        init(RandomizerClient.class, this::createHttpRandomizer);
        GreetingService greetingService = initAndRegisterJaxRsWsComponent(GreetingService.class, this::createGreetingService);
        GreetingResource greetingResource = initAndRegisterJaxRsWsComponent(GreetingResource.class, this::createGreetingResource);
        initHealth(new HealthProbe("greeting.request.count", greetingResource::getRequestCount));
    }

    private JaxRsOpenApiResource createOpenApiResource() {
        Info info = new Info()
                .title("Greeting API")
                .description("RESTful greetings for you.");
        String contextPath = config.get("server.context-path");
        OpenAPI oas = new OpenAPI()
                .info(info)
                .addServersItem(new Server() {
                    @Override
                    public String getUrl() {
                        return "http://localhost:" + getBoundPort() + contextPath;
                    }
                })
                .addServersItem(new Server().url(contextPath));
        SwaggerConfiguration oasConfig = new SwaggerConfiguration()
                .openAPI(oas)
                .prettyPrint(true);
        JaxRsOpenApiResource openApiResource = (JaxRsOpenApiResource) new JaxRsOpenApiResource()
                .openApiConfiguration(oasConfig);
        return openApiResource;
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

    private GreetingService createGreetingService() {
        RandomizerClient randomizerClient = get(RandomizerClient.class);
        GreetingCandidateRepository greetingCandidateRepository = get(GreetingCandidateRepository.class);
        return new GreetingService(greetingCandidateRepository, randomizerClient);
    }

    private GreetingResource createGreetingResource() {
        GreetingService greetingService = get(GreetingService.class);
        return new GreetingResource(greetingService);
    }
}
