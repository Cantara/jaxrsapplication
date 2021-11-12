package no.cantara.jaxrsapp.health;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.cantara.jaxrsapp.security.SecurityOverride;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;

@Path("/health")
public class HealthResource {

    public static final io.swagger.v3.oas.models.security.SecurityRequirement securityRequirement = new io.swagger.v3.oas.models.security.SecurityRequirement();

    private static final Logger log = LoggerFactory.getLogger(HealthResource.class);

    private final HealthService healthService;
    private final ObjectMapper mapper = new ObjectMapper();

    public HealthResource(HealthService healthService) {
        this.healthService = healthService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @SecurityOverride(logAccess = false) // disable authorization requirement in security-filter and do not trace-log access to this endpoint
    @SecurityRequirements(@SecurityRequirement(name = "none")) // disable authorization requirement in openapi spec
    public Response getHealth() {
        try {
            String currentHealthJsonWithoutTimestamp = healthService.getCurrentHealthJson();
            ObjectNode health = (ObjectNode) mapper.readTree(currentHealthJsonWithoutTimestamp);
            long healthComputeTimeMs = healthService.getHealthComputeTimeMs();
            boolean activelyUpdatingCurrentHealth = healthService.isActivelyUpdatingCurrentHealth();
            if (!activelyUpdatingCurrentHealth) {
                health.put("Status", "FAIL");
                health.put("errorMessage", "health-updater-thread is dead.");
            }
            health.put("now", Instant.now().toString());
            health.put("health-compute-time-ms", String.valueOf(healthComputeTimeMs));
            health.put("health-updater-thread-alive", String.valueOf(activelyUpdatingCurrentHealth));
            return Response.status(Response.Status.OK).entity(health).build();
        } catch (Throwable t) {
            log.error("While getting health", t);
            ObjectNode health = mapper.createObjectNode();
            health.put("Status", "FAIL");
            health.put("errorMessage", "While getting health");
            StringWriter strWriter = new StringWriter();
            t.printStackTrace(new PrintWriter(strWriter));
            health.put("errorCause", strWriter.toString());
            return Response.status(Response.Status.OK).entity(health).build();
        }
    }
}

