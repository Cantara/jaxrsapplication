package no.cantara.jaxrsapp.test;

import no.cantara.jaxrsapp.JaxRsServletApplication;

public interface AfterInitLifecycleListener {

    void afterInit(JaxRsServletApplication application);

}
