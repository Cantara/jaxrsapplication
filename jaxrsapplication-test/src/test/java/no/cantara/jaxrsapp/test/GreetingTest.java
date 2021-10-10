package no.cantara.jaxrsapp.test;

import jakarta.inject.Inject;
import no.cantara.jaxrsapp.test.greet.Greeting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JaxRsApplicationProvider("greet")
@ExtendWith(IntegrationTestExtension.class)
public class GreetingTest {

    private static final Logger log = LoggerFactory.getLogger(GreetingTest.class);

    @Inject
    TestClient testClient;

    @Test
    public void thatGreetingCanBeTestedByItself() {
        Greeting greeting = testClient.get(Greeting.class, "/greet/John").expect200Ok().body();
        log.info("Response: {}", greeting);
    }
}