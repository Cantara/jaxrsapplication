package no.cantara.jaxrsapp.sample.randomizer;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Inject;
import no.cantara.jaxrsapp.test.IntegrationTestExtension;
import no.cantara.jaxrsapp.test.TestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(IntegrationTestExtension.class)
public class RandomizerTest {

    private static final Logger log = LoggerFactory.getLogger(RandomizerTest.class);

    @Inject
    TestClient testClient;

    @Test
    public void thatViewerCanDoAllExceptReseed() {
        testClient.useAuthorization("Bearer fake-application-id: junit-viewer");
        log.info("GET /randomizer/str/10 Response: {}", testClient.get(String.class, "/randomizer/str/10").expect200Ok().body());
        log.info("GET /randomizer/int/1000 Response: {}", testClient.get(String.class, "/randomizer/int/1000").expect200Ok().body());
        log.info("GET /randomizer/health Response: {}", testClient.get(JsonNode.class, "/randomizer/health").expect200Ok().body().toPrettyString());
        testClient.put(JsonNode.class, "/randomizer/seed/12345").expect403Forbidden();
    }

    @Test
    public void thatAdminCanDoAllExceptReseed() {
        testClient.useAuthorization("Bearer fake-application-id: junit-admin");
        log.info("GET /randomizer/str/10 Response: {}", testClient.get(String.class, "/randomizer/str/10").expect200Ok().body());
        log.info("GET /randomizer/int/1000 Response: {}", testClient.get(String.class, "/randomizer/int/1000").expect200Ok().body());
        log.info("GET /randomizer/health Response: {}", testClient.get(JsonNode.class, "/randomizer/health").expect200Ok().body().toPrettyString());
        testClient.put(JsonNode.class, "/randomizer/seed/12345").expect200Ok();
    }

    @Test
    public void thatOpenApiWorks() {
        String openApiYaml = testClient.get(String.class, "/randomizer/openapi.yaml").expect200Ok().body();
        log.info("/randomizer/openapi.yaml:\n{}", openApiYaml);
    }
}