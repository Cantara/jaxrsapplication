package no.cantara.jaxrsapp.sample.integrationtests;

import no.cantara.jaxrsapp.test.MockRegistry;

import java.security.SecureRandom;
import java.util.Random;

public class RandomizerMockRegistry extends MockRegistry {

    public RandomizerMockRegistry() {
        addFactory(Random.class, this::createSecureRandom);
    }

    private Random createSecureRandom() {
        return new SecureRandom();
    }
}
