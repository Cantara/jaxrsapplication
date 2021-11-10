package no.cantara.jaxrsapp.sample.integrationtests;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import no.cantara.jaxrsapp.JaxRsRegistry;
import no.cantara.jaxrsapp.JaxRsServletApplication;
import no.cantara.jaxrsapp.sample.greeter.DefaultGreetingCandidateRepository;
import no.cantara.jaxrsapp.sample.greeter.Greeting;
import no.cantara.jaxrsapp.sample.greeter.GreetingCandidateRepository;
import no.cantara.jaxrsapp.sample.greeter.GreetingService;
import no.cantara.jaxrsapp.test.BeforeInitLifecycleListener;
import no.cantara.jaxrsapp.test.ConfigOverride;
import no.cantara.jaxrsapp.test.ConfigOverrides;
import no.cantara.jaxrsapp.test.IntegrationTestExtension;
import no.cantara.jaxrsapp.test.JaxRsApplicationProvider;
import no.cantara.jaxrsapp.test.TestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;


@JaxRsApplicationProvider({"greeter", "randomizer"})
@ExtendWith(IntegrationTestExtension.class)
@ConfigOverrides({
        @ConfigOverride(application = "greeter", value = {
                "randomizer.host", "localhost",
                "randomizer.port", "${randomizer.port}"
        })
})
@ConfigOverride(value = {
        "authentication.provider", "fake",
        "server.port", "0"
})
public class GreetAndRandomizerIntegrationTest implements BeforeInitLifecycleListener {

    private static final Logger log = LoggerFactory.getLogger(GreetAndRandomizerIntegrationTest.class);

    @Inject
    @Named("greeter")
    TestClient greeterClient;

    @Inject
    @Named("greeter")
    JaxRsServletApplication greeterApplication;

    @Inject
    @Named("randomizer")
    TestClient randomizerClient;

    @Override
    public void beforeInit(JaxRsServletApplication application) {
        // global (factory used by all applications)
        application.override(PrintWriter.class, () -> new PrintWriter(System.out));

        String alias = application.alias();
        if ("greeter".equals(alias)) {
            application.override(GreetingCandidateRepository.class, () -> createGreetingCandidateRepository(application));
        } else if ("randomizer".equals(alias)) {
            application.override(Random.class, SecureRandom::new);
        } else {
            Assertions.fail(String.format("Unknown application initialized. Alias: '%s'", alias));
        }
    }

    private GreetingCandidateRepository createGreetingCandidateRepository(JaxRsRegistry registry) {
        return new DefaultGreetingCandidateRepository(
                List.of("Mock-1", "Mock-2", "Mock-3", "Mock-4", "Mock-5", "Mock-6", "Mock-7", "Mock-8", "Mock-9", "Mock-10")
        );
    }

    @Test
    public void thatHealthOfGreetingAndRandomizerBothWorks() {
        JsonNode greeterHealth = greeterClient.get().path("/health").execute().expect200Ok().contentAsType(JsonNode.class);
        log.info("/health Response: {}", greeterHealth);
        JsonNode randomizerHealth = randomizerClient.get().path("/randomizer/health").execute().expect200Ok().contentAsType(JsonNode.class);
        log.info("/randomizer/health Response: {}", randomizerHealth);
    }

    @Test
    public void thatGreetingRestAPICanUseRandomizerToPickGreeting() {
        for (int i = 0; i < 10; i++) {
            Greeting greeting = greeterClient.get().path("/greet/John").fakeApplicationAuth("inttest-viewer").execute()
                    .expect200Ok().contentAsType(Greeting.class);
            log.info("/greet/John Response: {}", greeting);
            assertTrue(greeting.getGreeting().startsWith("Mock-")); // assert that mocks are used
        }
    }

    @Test
    public void thatGreetingServiceCanUseRandomizerToPick() {
        GreetingService greetingService = greeterApplication.get(GreetingService.class);
        for (int i = 0; i < 10; i++) {
            Greeting greeting = greetingService.greet("John", "fake-application-id: inttest-viewer");
            log.info("GreetingService.greet(\"John\"): {}", greeting);
            assertTrue(greeting.getGreeting().startsWith("Mock-")); // assert that mocks are used
        }
    }

    @Test
    public void thatGreetingOpenApiWorks() {
        String openApiYaml = greeterClient.get().path("/openapi.yaml").execute().expect200Ok().contentAsString();
        log.info("/openapi.yaml:\n{}", openApiYaml);
    }

    @Test
    public void thatRandomizerOpenApiWorks() {
        String openApiYaml = randomizerClient.get().path("/randomizer/openapi.yaml").execute().expect200Ok().contentAsString();
        log.info("/randomizer/openapi.yaml:\n{}", openApiYaml);
    }
}
