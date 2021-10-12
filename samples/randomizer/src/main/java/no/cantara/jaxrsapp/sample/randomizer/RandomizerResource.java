package no.cantara.jaxrsapp.sample.randomizer;


import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.cantara.jaxrsapp.security.SecureAction;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

@Path("/")
public class RandomizerResource {

    private final AtomicLong requestCount = new AtomicLong();
    private final Random random = new Random(System.currentTimeMillis());

    public RandomizerResource() {
    }

    public long getRequestCount() {
        return requestCount.get();
    }

    @GET
    @Path("/str/{length}")
    @Produces(MediaType.APPLICATION_JSON)
    @SecureAction("getstr")
    public Response getstr(@PathParam("length") String lengthParam) {
        requestCount.incrementAndGet();
        final String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        int N = Math.min(Integer.parseInt(lengthParam), 100);
        char[] state = new char[N];
        for (int i = 0; i < N; i++) {
            int a = random.nextInt(alphabet.length());
            state[i] = alphabet.charAt(a);
        }
        return Response.ok(new String(state), MediaType.TEXT_PLAIN_TYPE).build();
    }

    @GET
    @Path("/int/{bound}")
    @Produces(MediaType.APPLICATION_JSON)
    @SecureAction("getint")
    public Response getint(@PathParam("bound") String upperBoundExParam) {
        requestCount.incrementAndGet();
        int randomResult = random.nextInt(Integer.parseInt(upperBoundExParam));
        return Response.ok(String.valueOf(randomResult), MediaType.TEXT_PLAIN_TYPE).build();
    }

    @PUT
    @Path("/seed/{seed}")
    @Produces(MediaType.APPLICATION_JSON)
    @SecureAction("reseed")
    public Response reseed(@PathParam("seed") String seedParam) {
        requestCount.incrementAndGet();
        long seed = Long.parseLong(seedParam);
        random.setSeed(seed);
        return Response.ok().build();
    }
}
