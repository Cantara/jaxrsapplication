package no.cantara.jaxrsapp.test;

import no.cantara.jaxrsapp.test.greet.Greeting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

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