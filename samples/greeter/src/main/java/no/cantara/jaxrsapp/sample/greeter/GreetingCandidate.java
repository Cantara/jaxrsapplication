package no.cantara.jaxrsapp.sample.greeter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GreetingCandidate {

    private final String greeting;

    @JsonCreator
    public GreetingCandidate(@JsonProperty("greeting") String greeting) {
        this.greeting = greeting;
    }

    public String getGreeting() {
        return greeting;
    }
}
