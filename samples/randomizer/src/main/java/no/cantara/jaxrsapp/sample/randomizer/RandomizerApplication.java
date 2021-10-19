package no.cantara.jaxrsapp.sample.randomizer;

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
        initAndAddServletFilter(CORSServletFilter.class, CORSServletFilter::new, "/*", EnumSet.allOf(DispatcherType.class));
        initAndRegisterJaxRsWsComponent(JaxRsOpenApiResource.class.getName(), this::createOpenApiResource);
        PrintWriter pw = init(PrintWriter.class, this::createAuditTo);
        pw.printf("AUDIT: I am the Randomizer application!%n").flush();
        init(Random.class, this::createRandom);
        RandomizerResource randomizerResource = initAndRegisterJaxRsWsComponent(RandomizerResource.class, this::createRandomizerResource);
        initHealth(new HealthProbe("randomizer.request.count", randomizerResource::getRequestCount));
    }

    private JaxRsOpenApiResource createOpenApiResource() {
        Info info = new Info()
                .title("Randomizer API")
                .description("RESTful random generator");
        OpenAPI oas = new OpenAPI()
                .info(info)
                .addServersItem(new Server().url("/" + alias()))
                .addServersItem(new Server() {
                    @Override
                    public String getUrl() {
                        return "http://localhost:" + getBoundPort() + "/" + alias();
                    }
                });
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

    private Random createRandom() {
        return new Random(System.currentTimeMillis());
    }

    private RandomizerResource createRandomizerResource() {
        Random random = get(Random.class);
        return new RandomizerResource(random);
    }
}
