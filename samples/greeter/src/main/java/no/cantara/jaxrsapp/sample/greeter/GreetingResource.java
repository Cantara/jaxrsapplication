package no.cantara.jaxrsapp.sample.greeter;


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

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Path("/")
public class GreetingResource {

    private final AtomicLong requestCount = new AtomicLong();
    private final GreetingCandidateRepository greetingCandidateRepository;
    private final RandomizerClient randomizerClient;

    public GreetingResource(GreetingCandidateRepository greetingCandidateRepository, RandomizerClient randomizerClient) {
        this.greetingCandidateRepository = greetingCandidateRepository;
        this.randomizerClient = randomizerClient;
    }

    public long getRequestCount() {
        return requestCount.get();
    }

    @GET
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @SecureAction("greet")
    public Greeting greet(@PathParam("name") String name, @QueryParam("greeting") String greetingParam, @Context SecurityContext securityContext) {
        requestCount.incrementAndGet();
        String greeting;
        if (greetingParam != null) {
            greeting = greetingParam;
        } else {
            JaxRsAppPrincipal principal = (JaxRsAppPrincipal) securityContext.getUserPrincipal();
            String forwardingToken = principal.getAuthentication().forwardingToken();
            List<GreetingCandidate> greetingCandidates = greetingCandidateRepository.greetingCandidates();
            int randomizedCandidateIndex = randomizerClient.getRandomInteger(forwardingToken, greetingCandidates.size());
            greeting = greetingCandidates.get(randomizedCandidateIndex).greeting;
        }
        return new Greeting(name, greeting);
    }
}
