package no.cantara.jaxrsapp.test.notes;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

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
