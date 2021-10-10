package no.cantara.jaxrsapp.test.greet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Greeting {

    final String name;
    final String greeting;

    @JsonCreator
    public Greeting(@JsonProperty("name") String name, @JsonProperty("greeting") String greeting) {
        this.name = name;
        this.greeting = greeting;
    }

    public String getName() {
        return name;
    }

    public String getGreeting() {
        return greeting;
    }

    @Override
    public String toString() {
        return "Greeting{" +
                "name='" + name + '\'' +
                ", greeting='" + greeting + '\'' +
                '}';
    }
}
