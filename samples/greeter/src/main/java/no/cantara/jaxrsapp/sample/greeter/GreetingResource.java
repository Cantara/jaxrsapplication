package no.cantara.jaxrsapp.sample.greeter;


import com.codahale.metrics.annotation.Timed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import no.cantara.jaxrsapp.security.JaxRsAppPrincipal;
import no.cantara.jaxrsapp.security.SecureAction;

import java.util.concurrent.atomic.AtomicLong;

@Path("/greet")
public class GreetingResource {

    private final AtomicLong requestCount = new AtomicLong();
    private final GreetingService greetingService;

    public GreetingResource(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    public long getRequestCount() {
        return requestCount.get();
    }

    @GET
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @SecureAction("greet")
    @Timed
    public Greeting greet(@PathParam("name") String name, @QueryParam("greeting") String greetingParam, @Context SecurityContext securityContext) {
        requestCount.incrementAndGet();
        if (greetingParam != null) {
            return new Greeting(name, greetingParam);
        }
        JaxRsAppPrincipal principal = (JaxRsAppPrincipal) securityContext.getUserPrincipal();
        String forwardingToken = principal.getAuthentication().forwardingToken();
        Greeting greeting = greetingService.greet(name, forwardingToken);
        return greeting;
    }
}
