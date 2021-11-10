package no.cantara.jaxrsapp.sample.greeter;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class DefaultGreetingCandidateRepository implements GreetingCandidateRepository {

    final List<GreetingCandidate> candidates = new CopyOnWriteArrayList<>();

    public DefaultGreetingCandidateRepository(List<String> greetingCandidates) {
        greetingCandidates.stream()
                .map(GreetingCandidate::new)
                .forEach(candidates::add);
    }

    public List<GreetingCandidate> greetingCandidates() {
        return Collections.unmodifiableList(candidates);
    }

    public void addCandidate(GreetingCandidate candidate) {
        Objects.requireNonNull(candidate);
        candidates.add(candidate);
    }
}
