package no.cantara.jaxrsapp.sample.greeter;

import no.cantara.jaxrsapp.JaxRsRegistry;

import java.util.Objects;

public class RandomizerClientMock {

    static RandomizerClient createMockRandomizer(JaxRsRegistry registry) {
        GreetingCandidateRepository greetingCandidateRepository = registry.get(GreetingCandidateRepository.class);
        Objects.requireNonNull(greetingCandidateRepository); // test that registry is wired correctly
        return new RandomizerClient() {
            @Override
            public String getRandomString(String token, int maxLength) {
                return greetingCandidateRepository.greetingCandidates().get(0).getGreeting();
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
