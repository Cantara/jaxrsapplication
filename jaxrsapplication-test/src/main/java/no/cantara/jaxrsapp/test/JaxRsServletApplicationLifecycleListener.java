package no.cantara.jaxrsapp.test;

import no.cantara.jaxrsapp.JaxRsServletApplication;

public interface JaxRsServletApplicationLifecycleListener {

    void beforeInit(JaxRsServletApplication application);

}
