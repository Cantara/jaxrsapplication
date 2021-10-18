package no.cantara.jaxrsapp.test;

import no.cantara.jaxrsapp.JaxRsServletApplication;

public interface AfterPostInitLifecycleListener {

    void afterPostInit(JaxRsServletApplication application);

}
