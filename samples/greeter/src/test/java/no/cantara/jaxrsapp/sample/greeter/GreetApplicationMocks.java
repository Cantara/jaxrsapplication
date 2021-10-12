package no.cantara.jaxrsapp.sample.greeter;

import no.cantara.jaxrsapp.test.MockRegistry;

public class GreetApplicationMocks extends MockRegistry {

    public GreetApplicationMocks() {
        add(RandomizerClient.class, createMockRandomizer());
    }

    private RandomizerClient createMockRandomizer() {
        return new RandomizerClient() {
            @Override
            public String getRandomString(String token, int maxLength) {
                return "mock";
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
