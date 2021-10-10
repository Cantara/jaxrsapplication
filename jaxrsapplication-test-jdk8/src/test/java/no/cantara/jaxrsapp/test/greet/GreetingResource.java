package no.cantara.jaxrsapp.test.greet;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

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
