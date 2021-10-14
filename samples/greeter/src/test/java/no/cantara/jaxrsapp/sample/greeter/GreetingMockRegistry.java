package no.cantara.jaxrsapp.sample.greeter;

import no.cantara.jaxrsapp.test.MockRegistry;

import java.util.Objects;

public class GreetingMockRegistry extends MockRegistry {

    public GreetingMockRegistry() {
        addFactory(RandomizerClient.class, this::createMockRandomizer);
    }

    private RandomizerClient createMockRandomizer() {
        GreetingCandidateRepository greetingCandidateRepository = get(GreetingCandidateRepository.class);
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
