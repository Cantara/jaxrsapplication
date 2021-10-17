package no.cantara.jaxrsapp.sample.greeter;

import java.util.List;

public class GreetingService {

    private final GreetingCandidateRepository greetingCandidateRepository;
    private final RandomizerClient randomizerClient;

    public GreetingService(GreetingCandidateRepository greetingCandidateRepository, RandomizerClient randomizerClient) {
        this.greetingCandidateRepository = greetingCandidateRepository;
        this.randomizerClient = randomizerClient;
    }

    public Greeting greet(String name, String forwardingToken) {
        List<GreetingCandidate> greetingCandidates = greetingCandidateRepository.greetingCandidates();
        int randomizedCandidateIndex = randomizerClient.getRandomInteger(forwardingToken, greetingCandidates.size());
        String greeting = greetingCandidates.get(randomizedCandidateIndex).greeting;
        return new Greeting(name, greeting);
    }
}
