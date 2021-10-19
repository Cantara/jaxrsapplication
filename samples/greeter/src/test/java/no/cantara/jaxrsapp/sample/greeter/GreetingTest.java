package no.cantara.jaxrsapp.sample.greeter;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Inject;
import no.cantara.jaxrsapp.JaxRsServletApplication;
import no.cantara.jaxrsapp.test.BeforeInitLifecycleListener;
import no.cantara.jaxrsapp.test.IntegrationTestExtension;
import no.cantara.jaxrsapp.test.TestClient;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(IntegrationTestExtension.class)
public class GreetingTest implements BeforeInitLifecycleListener {

    private static final Logger log = LoggerFactory.getLogger(GreetingTest.class);

    @Inject
    TestClient testClient;

    @Override
    public void beforeInit(JaxRsServletApplication application) {
        application.override(RandomizerClient.class, () -> RandomizerClientMock.createMockRandomizer(application));
    }

    @Test
    public void thatGreetingCanBeTestedByItself() {
        Greeting greeting = testClient.get(Greeting.class, "/greet/John",
                        HttpHeaders.AUTHORIZATION, "Bearer fake-application-id: junit-viewer")
                .expect200Ok().body();
        log.info("/greet/John Response: {}", greeting);
        JsonNode body = testClient.get(JsonNode.class, "/greet/health").expect200Ok().body();
        log.info("/greet/health Response: {}", body.toPrettyString());
    }

    @Test
    public void thatOpenApiWorks() {
        String openApiYaml = testClient.get(String.class, "/greet/openapi.yaml").expect200Ok().body();
        log.info("/greet/openapi.yaml:\n{}", openApiYaml);
    }
}