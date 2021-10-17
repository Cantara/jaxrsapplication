package no.cantara.jaxrsapp.sample.integrationtests.example1;

import no.cantara.jaxrsapp.JaxRsServletApplication;
import no.cantara.jaxrsapp.test.JaxRsServletApplicationLifecycleListener;

import java.security.SecureRandom;
import java.util.Random;

public class RandomizerLifecycleListener implements JaxRsServletApplicationLifecycleListener {

    @Override
    public void beforeInit(JaxRsServletApplication application) {
        application.override(Random.class, this::createSecureRandom);
    }

    private Random createSecureRandom() {
        return new SecureRandom();
    }
}
