package no.cantara.jaxrsapp.sample.integrationtests;

import no.cantara.jaxrsapp.test.MockRegistry;

import java.io.PrintWriter;

public class GlobalMockRegistry extends MockRegistry {

    public GlobalMockRegistry() {
        addFactory(PrintWriter.class, this::createAuditTo);
    }

    private PrintWriter createAuditTo() {
        return new PrintWriter(System.out);
    }
}
