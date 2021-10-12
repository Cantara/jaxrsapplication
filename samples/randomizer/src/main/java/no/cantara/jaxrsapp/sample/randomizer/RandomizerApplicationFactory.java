package no.cantara.jaxrsapp.sample.randomizer;

import no.cantara.config.ApplicationProperties;
import no.cantara.jaxrsapp.JaxRsServletApplicationFactory;

public class RandomizerApplicationFactory implements JaxRsServletApplicationFactory<RandomizerApplication> {

    @Override
    public Class<?> providerClass() {
        return RandomizerApplication.class;
    }

    @Override
    public String alias() {
        return "randomizer";
    }

    @Override
    public RandomizerApplication create(ApplicationProperties config) {
        return new RandomizerApplication(config);
    }
}
