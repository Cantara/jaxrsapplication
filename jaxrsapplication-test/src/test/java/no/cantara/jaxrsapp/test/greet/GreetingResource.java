package no.cantara.jaxrsapp.test.greet;


import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/")
public class GreetingResource {

    public GreetingResource() {
    }

    @GET
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Greeting greet(@PathParam("name") String name, @QueryParam("greeting") String greetingParam) {
        String greeting = "Hello";
        if (greetingParam != null) {
            greeting = greetingParam;
        }
        return new Greeting(name, greeting);
    }
}
