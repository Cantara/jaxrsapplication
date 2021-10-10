package no.cantara.jaxrsapp.test.notes;

import no.cantara.config.ApplicationProperties;
import no.cantara.jaxrsapp.AbstractJaxRsServletApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotesApplication extends AbstractJaxRsServletApplication<NotesApplication> {

    private static final Logger log = LoggerFactory.getLogger(NotesApplication.class);

    public static void main(String[] args) {
        NotesApplication application = new NotesApplication(ApplicationProperties.builder().defaults().build());
        application.init();
        application.start();
    }

    public NotesApplication(ApplicationProperties config) {
        super(config);
    }

    @Override
    public NotesApplication init() {
        initAndRegisterJaxRsWsComponent(NoteResource.class, this::initNoteResource);
        return this;
    }

    private NoteResource initNoteResource() {
        return new NoteResource();
    }
}
