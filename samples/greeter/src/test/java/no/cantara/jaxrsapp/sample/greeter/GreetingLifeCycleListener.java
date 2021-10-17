package no.cantara.jaxrsapp.sample.greeter;

import no.cantara.jaxrsapp.JaxRsRegistry;
import no.cantara.jaxrsapp.JaxRsServletApplication;
import no.cantara.jaxrsapp.test.JaxRsServletApplicationLifecycleListener;

import java.util.Objects;

public class GreetingLifeCycleListener implements JaxRsServletApplicationLifecycleListener {

    @Override
    public void beforeInit(JaxRsServletApplication application) {
        application.override(RandomizerClient.class, () -> createMockRandomizer(application));
    }

    private RandomizerClient createMockRandomizer(JaxRsRegistry registry) {
        GreetingCandidateRepository greetingCandidateRepository = registry.get(GreetingCandidateRepository.class);
        Objects.requireNonNull(greetingCandidateRepository); // test that registry is wired correctly
        return new RandomizerClient() {
            @Override
            public String getRandomString(String token, int maxLength) {
                return greetingCandidateRepository.greetingCandidates().get(0).greeting;
            }

            @Override
            public int getRandomInteger(String token, int upperBoundEx) {
                return 0;
            }

            @Override
            public void reseed(String token, long seed) {
            }
        };
    }
}
