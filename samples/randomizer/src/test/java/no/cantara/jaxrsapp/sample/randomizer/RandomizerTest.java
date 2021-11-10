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
        testClient.useFakeApplicationAuth("junit-viewer");
        log.info("GET /randomizer/str/10 Response: {}", testClient.get().path("/randomizer/str/10").execute().expect200Ok().contentAsString());
        log.info("GET /randomizer/int/1000 Response: {}", testClient.get().path("/randomizer/int/1000").execute().expect200Ok().contentAsString());
        log.info("GET /randomizer/health Response: {}", testClient.get().path("/randomizer/health").execute().expect200Ok().contentAsType(JsonNode.class).toPrettyString());
        testClient.put().path("/randomizer/seed/12345").execute().expect403Forbidden();
    }

    @Test
    public void thatAdminCanDoAll() {
        testClient.useFakeApplicationAuth("junit-admin");
        log.info("GET /randomizer/str/10 Response: {}", testClient.get().path("/randomizer/str/10").execute().expect200Ok().contentAsString());
        log.info("GET /randomizer/int/1000 Response: {}", testClient.get().path("/randomizer/int/1000").execute().expect200Ok().contentAsString());
        log.info("GET /randomizer/health Response: {}", testClient.get().path("/randomizer/health").execute().expect200Ok().contentAsType(JsonNode.class).toPrettyString());
        testClient.put().path("/randomizer/seed/12345").execute().expect200Ok();
    }

    @Test
    public void thatOpenApiWorks() {
        String openApiYaml = testClient.get().path("/randomizer/openapi.yaml").execute().expect200Ok().contentAsString();
        log.info("/randomizer/openapi.yaml:\n{}", openApiYaml);
    }
}