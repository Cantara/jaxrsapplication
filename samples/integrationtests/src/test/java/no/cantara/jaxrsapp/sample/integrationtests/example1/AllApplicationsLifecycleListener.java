package no.cantara.jaxrsapp.sample.integrationtests.example1;

import no.cantara.jaxrsapp.JaxRsServletApplication;
import no.cantara.jaxrsapp.test.JaxRsServletApplicationLifecycleListener;

import java.io.PrintWriter;

public class AllApplicationsLifecycleListener implements JaxRsServletApplicationLifecycleListener {

    @Override
    public void beforeInit(JaxRsServletApplication application) {
        application.override(PrintWriter.class, this::createAuditTo);
    }

    private PrintWriter createAuditTo() {
        return new PrintWriter(System.out);
    }
}
