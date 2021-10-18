package no.cantara.jaxrsapp.test;

import no.cantara.jaxrsapp.JaxRsServletApplication;

public interface BeforeInitLifecycleListener {

    void beforeInit(JaxRsServletApplication application);

}
