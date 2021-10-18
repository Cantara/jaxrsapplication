package no.cantara.jaxrsapp.test;

import no.cantara.jaxrsapp.JaxRsServletApplication;

public interface AfterStartLifecycleListener {

    void afterStart(JaxRsServletApplication application);

}
