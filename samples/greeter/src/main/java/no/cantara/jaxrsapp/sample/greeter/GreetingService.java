package no.cantara.jaxrsapp.sample.greeter;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

public class GreetingService {

    private final MetricRegistry metricRegistry;
    private final GreetingCandidateRepository greetingCandidateRepository;
    private final RandomizerClient randomizerClient;
    private final Map<String, Metric> candidateCountersMap = new ConcurrentSkipListMap<>();

    public GreetingService(MetricRegistry metricRegistry, GreetingCandidateRepository greetingCandidateRepository, RandomizerClient randomizerClient) {
        this.metricRegistry = metricRegistry;
        this.greetingCandidateRepository = greetingCandidateRepository;
        this.randomizerClient = randomizerClient;
    }

    public Greeting greet(String name, String forwardingToken) {
        List<GreetingCandidate> greetingCandidates = greetingCandidateRepository.greetingCandidates();
        int randomizedCandidateIndex = randomizerClient.getRandomInteger(forwardingToken, greetingCandidates.size());
        String greeting = greetingCandidates.get(randomizedCandidateIndex).getGreeting();
        Counter counter = (Counter) candidateCountersMap.computeIfAbsent(greeting, g -> metricRegistry.register("greeting." + g, new Counter()));
        counter.inc(); // increment number of times this greeting candidate was chosen
        return new Greeting(name, greeting);
    }

    public void addCandidate(GreetingCandidate candidate) {
        greetingCandidateRepository.addCandidate(candidate);
    }

    public List<GreetingCandidate> listCandidates() {
        return greetingCandidateRepository.greetingCandidates();
    }
}
