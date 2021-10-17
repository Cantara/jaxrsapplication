package no.cantara.jaxrsapp.sample.integrationtests.example1;

import no.cantara.jaxrsapp.JaxRsRegistry;
import no.cantara.jaxrsapp.JaxRsServletApplication;
import no.cantara.jaxrsapp.sample.greeter.GreetingCandidate;
import no.cantara.jaxrsapp.sample.greeter.GreetingCandidateRepository;
import no.cantara.jaxrsapp.test.JaxRsServletApplicationLifecycleListener;
import no.cantara.security.authorization.AccessManager;

import java.util.List;
import java.util.Objects;

public class GreetingLifecycleListener implements JaxRsServletApplicationLifecycleListener {

    @Override
    public void beforeInit(JaxRsServletApplication application) {
        application.override(GreetingCandidateRepository.class, () -> createGreetingCandidateRepository(application));
    }

    private GreetingCandidateRepository createGreetingCandidateRepository(JaxRsRegistry registry) {
        AccessManager accessManager = registry.get(AccessManager.class);
        Objects.requireNonNull(accessManager); // assert that security has been initialized
        return new GreetingCandidateRepository() {
            @Override
            public List<GreetingCandidate> greetingCandidates() {
                return List.of(
                        new GreetingCandidate("Mock-1"),
                        new GreetingCandidate("Mock-2"),
                        new GreetingCandidate("Mock-3"),
                        new GreetingCandidate("Mock-4"),
                        new GreetingCandidate("Mock-5"),
                        new GreetingCandidate("Mock-6"),
                        new GreetingCandidate("Mock-7"),
                        new GreetingCandidate("Mock-8"),
                        new GreetingCandidate("Mock-9"),
                        new GreetingCandidate("Mock-10")
                );
            }
        };
    }
}
