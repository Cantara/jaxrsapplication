package no.cantara.jaxrsapp.test;

import no.cantara.jaxrsapp.JaxRsServletApplication;
import no.cantara.jaxrsapp.test.greet.Greeting;
import no.cantara.jaxrsapp.test.greet.GreetingResource;
import no.cantara.jaxrsapp.test.notes.Note;
import no.cantara.jaxrsapp.test.notes.NoteResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;

@JaxRsApplicationProvider({"greet", "notes"})
@ExtendWith(IntegrationTestExtension.class)
public class MultiApplicationsJdk8Test {

    private static final Logger log = LoggerFactory.getLogger(MultiApplicationsJdk8Test.class);

    @Inject
    @Named("greet")
    TestClient greetTestClient;

    @Inject
    @Named("greet")
    JaxRsServletApplication greetingApplication;

    @Inject
    @Named("notes")
    TestClient noteTestClient;

    @Inject
    @Named("notes")
    JaxRsServletApplication notesApplication;

    @Test
    public void thatBothGreetingAndNotesApplicationsCanBeTestedAtTheSameTimeJdk8() {
        Greeting greeting = greetTestClient.get(Greeting.class, "/greet/John").expect200Ok().body();
        log.info("Greeting Response: {}", greeting);
        GreetingResource greetingResource = (GreetingResource) greetingApplication.get(GreetingResource.class);
        log.info("GreetingResource directly: " + greetingResource.greet("Jane", null));
        Note note = noteTestClient.get(Note.class, "/note/hei").expect200Ok().body();
        log.info("Note Response: {}", note);
        NoteResource noteResource = (NoteResource) notesApplication.get(NoteResource.class);
        log.info("NoteResource directly: " + noteResource.hei());
    }
}