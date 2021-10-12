package no.cantara.jaxrsapp.sample.greeter;

public interface RandomizerClient {

    String getRandomString(String token, int maxLength);

    int getRandomInteger(String token, int upperBoundEx);

    void reseed(String token, long seed);
}
