package no.cantara.jaxrsapp.sample.integrationtests;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import no.cantara.jaxrsapp.sample.greeter.Greeting;
import no.cantara.jaxrsapp.test.IntegrationTestExtension;
import no.cantara.jaxrsapp.test.JaxRsApplicationProvider;
import no.cantara.jaxrsapp.test.TestClient;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JaxRsApplicationProvider({"greeter", "randomizer"})
@ExtendWith(IntegrationTestExtension.class)
public class GreetAndRandomizerIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(GreetAndRandomizerIntegrationTest.class);

    @Inject
    @Named("greeter")
    TestClient greeterClient;

    @Inject
    @Named("randomizer")
    TestClient randomizerClient;

    @Test
    public void thatGreetingCanUseRandomizerToPickGreeting() {
        for (int i = 0; i < 10; i++) {
            Greeting greeting = greeterClient.get(Greeting.class, "/greet/John",
                            HttpHeaders.AUTHORIZATION, "Bearer fake-application-id: inttest-viewer")
                    .expect200Ok().body();
            log.info("/greet/John Response: {}", greeting);
        }
        JsonNode greeterHealth = greeterClient.get(JsonNode.class, "/greet/health").expect200Ok().body();
        log.info("/greet/health Response: {}", greeterHealth);
        JsonNode randomizerHealth = randomizerClient.get(JsonNode.class, "/randomizer/health").expect200Ok().body();
        log.info("/randomizer/health Response: {}", randomizerHealth);
    }
}
