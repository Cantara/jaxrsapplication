package no.cantara.jaxrsapp.sample.greeter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(IntegrationTestExtension.class)
public class GreetingTest implements BeforeInitLifecycleListener {

    private static final Logger log = LoggerFactory.getLogger(GreetingTest.class);

    private static final ObjectMapper mapper = new ObjectMapper();

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
        JsonNode body = testClient.get(JsonNode.class, "/health").expect200Ok().body();
        log.info("/health Response: {}", body.toPrettyString());
    }

    @Test
    public void thatOpenApiWorks() {
        String openApiYaml = testClient.get(String.class, "/openapi.yaml").expect200Ok().body();
        log.info("/openapi.yaml:\n{}", openApiYaml);
    }

    @Test
    public void thatAdminHealthCheckServletWorks() throws JsonProcessingException {
        String body = testClient.get(String.class, "/admin/healthcheck").expect200Ok().body();
        log.info("/admin/healthcheck Response:\n{}", mapper.readValue(body, JsonNode.class).toPrettyString());
    }

    @Test
    public void thatAdminMetricsServletWorks() throws JsonProcessingException, InterruptedException {
        for (int i = 0; i < 50; i++) {
            int remainder = i % 4;
            if (remainder == 0) {
                testClient.get(String.class, "/admin/ping").expect200Ok().body();
            } else if (remainder == 1) {
                testClient.get(String.class, "/admin/healthcheck").expect200Ok().body();
            } else if (remainder == 2) {
                testClient.get(String.class, "/greet/John", HttpHeaders.AUTHORIZATION, "Bearer fake-application-id: junit-viewer").expect200Ok().body();
            } else {
                // remainder == 3
                testClient.get(String.class, "/openapi.yaml").expect200Ok().body();
            }
        }
        String body = testClient.get(String.class, "/admin/metrics/jersey").expect200Ok().body();
        log.info("/admin/metrics/jersey Response:\n{}", mapper.readValue(body, JsonNode.class).toPrettyString());
    }

    @Test
    public void thatAdminPingServletWorks() {
        String body = testClient.get(String.class, "/admin/ping").expect200Ok().body();
        log.info("/admin/ping Response: '{}'", body);
    }

    @Test
    public void thatAdminThreadsServletWorks() {
        String body = testClient.get(String.class, "/admin/threads").expect200Ok().body();
        log.info("/admin/threads Response:\n{}", body);
    }

    @Test
    public void thatAdminCpuProfileServletWorks() {
        ByteBuffer buf = testClient.get(ByteBuffer.class, "/admin/pprof?duration=1").expect200Ok().body();
        assertTrue(buf.limit() > 0);
        System.out.printf("Bytes in body: %d%n", buf.limit());
    }
}