package no.cantara.jaxrsapp.test.notes;

import no.cantara.config.ApplicationProperties;
import no.cantara.jaxrsapp.JaxRsServletApplicationFactory;

public class NotesApplicationFactory implements JaxRsServletApplicationFactory<NotesApplication> {

    @Override
    public Class<?> providerClass() {
        return NotesApplication.class;
    }

    @Override
    public String alias() {
        return "notes";
    }

    @Override
    public NotesApplication create(ApplicationProperties applicationProperties) {
        return new NotesApplication(applicationProperties);
    }
}
