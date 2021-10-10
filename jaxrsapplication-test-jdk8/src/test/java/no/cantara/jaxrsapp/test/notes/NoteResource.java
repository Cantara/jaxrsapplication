package no.cantara.jaxrsapp.test.notes;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

@Path("/")
public class NoteResource {

    public NoteResource() {
    }

    @GET
    @Path("/hei")
    @Produces(MediaType.APPLICATION_JSON)
    public Note hei() {
        return new Note(UUID.randomUUID().toString(), "Hello");
    }
}
