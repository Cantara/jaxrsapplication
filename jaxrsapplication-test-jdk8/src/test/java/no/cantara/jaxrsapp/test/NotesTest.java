package no.cantara.jaxrsapp.test;

import no.cantara.jaxrsapp.test.notes.Note;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

@JaxRsApplicationProvider("notes")
@ExtendWith(IntegrationTestExtension.class)
public class NotesTest {

    private static final Logger log = LoggerFactory.getLogger(NotesTest.class);

    @Inject
    TestClient testClient;

    @Test
    public void thatNotesCanBeTestedByItself() {
        Note note = testClient.get(Note.class, "/note/hei").expect200Ok().body();
        log.info("BODY: '{}'", note);
    }
}