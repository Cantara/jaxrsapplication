package no.cantara.jaxrsapp.sample.integrationtests.example2;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import no.cantara.jaxrsapp.JaxRsRegistry;
import no.cantara.jaxrsapp.JaxRsServletApplication;
import no.cantara.jaxrsapp.sample.greeter.Greeting;
import no.cantara.jaxrsapp.sample.greeter.GreetingCandidate;
import no.cantara.jaxrsapp.sample.greeter.GreetingCandidateRepository;
import no.cantara.jaxrsapp.sample.greeter.GreetingService;
import no.cantara.jaxrsapp.test.ConfigOverride;
import no.cantara.jaxrsapp.test.ConfigOverrides;
import no.cantara.jaxrsapp.test.IntegrationTestExtension;
import no.cantara.jaxrsapp.test.JaxRsApplicationProvider;
import no.cantara.jaxrsapp.test.JaxRsServletApplicationLifecycleListener;
import no.cantara.jaxrsapp.test.TestClient;
import no.cantara.security.authorization.AccessManager;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.security.SecureRandom;
import java.util.List;
import java.util.Objects;
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
public class GreetAndRandomizerExample2IntegrationTest implements JaxRsServletApplicationLifecycleListener {

    private static final Logger log = LoggerFactory.getLogger(GreetAndRandomizerExample2IntegrationTest.class);

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
        AccessManager accessManager = registry.get(AccessManager.class);
        Objects.requireNonNull(accessManager); // assert that security has been initialized
        return new GreetingCandidateRepository() {
            @Override
            public List<GreetingCandidate> greetingCandidates() {
                return List.of(
                        new GreetingCandidate("Mock-1"),
                        new GreetingCandidate("Mock-2"),
                        new GreetingCandidate("Mock-3"),
                        new GreetingCandidate("Mock-4"),
                        new GreetingCandidate("Mock-5"),
                        new GreetingCandidate("Mock-6"),
                        new GreetingCandidate("Mock-7"),
                        new GreetingCandidate("Mock-8"),
                        new GreetingCandidate("Mock-9"),
                        new GreetingCandidate("Mock-10")
                );
            }
        };
    }

    @Test
    public void thatHealthOfGreetingAndRandomizerBothWorks() {
        JsonNode greeterHealth = greeterClient.get(JsonNode.class, "/greet/health").expect200Ok().body();
        log.info("/greet/health Response: {}", greeterHealth);
        JsonNode randomizerHealth = randomizerClient.get(JsonNode.class, "/randomizer/health").expect200Ok().body();
        log.info("/randomizer/health Response: {}", randomizerHealth);
    }

    @Test
    public void thatGreetingRestAPICanUseRandomizerToPickGreeting() {
        for (int i = 0; i < 10; i++) {
            Greeting greeting = greeterClient.get(Greeting.class, "/greet/John",
                            HttpHeaders.AUTHORIZATION, "Bearer fake-application-id: inttest-viewer")
                    .expect200Ok().body();
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
}
