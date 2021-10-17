package no.cantara.jaxrsapp.sample.integrationtests.example1;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import no.cantara.jaxrsapp.JaxRsServletApplication;
import no.cantara.jaxrsapp.sample.greeter.Greeting;
import no.cantara.jaxrsapp.sample.greeter.GreetingService;
import no.cantara.jaxrsapp.test.ApplicationLifecycleListenerConfig;
import no.cantara.jaxrsapp.test.ApplicationLifecycleListenerConfigs;
import no.cantara.jaxrsapp.test.ConfigOverride;
import no.cantara.jaxrsapp.test.ConfigOverrides;
import no.cantara.jaxrsapp.test.IntegrationTestExtension;
import no.cantara.jaxrsapp.test.JaxRsApplicationProvider;
import no.cantara.jaxrsapp.test.TestClient;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertTrue;


@JaxRsApplicationProvider({"greeter", "randomizer"})
@ExtendWith(IntegrationTestExtension.class)
@ApplicationLifecycleListenerConfigs({
        @ApplicationLifecycleListenerConfig(application = "greeter", value = GreetingLifecycleListener.class),
        @ApplicationLifecycleListenerConfig(application = "randomizer", value = RandomizerLifecycleListener.class)
})
@ApplicationLifecycleListenerConfig(AllApplicationsLifecycleListener.class)
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
public class GreetAndRandomizerExample1IntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(GreetAndRandomizerExample1IntegrationTest.class);

    @Inject
    @Named("greeter")
    TestClient greeterClient;

    @Inject
    @Named("greeter")
    JaxRsServletApplication greeterApplication;

    @Inject
    @Named("randomizer")
    TestClient randomizerClient;

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
