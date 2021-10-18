package no.cantara.jaxrsapp;

import no.cantara.config.ApplicationProperties;
import no.cantara.config.ProviderFactory;

import static java.util.Optional.ofNullable;

public interface JaxRsServletApplicationFactory<A extends JaxRsServletApplication<A>> extends ProviderFactory<JaxRsServletApplication<A>> {

    default ApplicationProperties.Builder conventions(ApplicationProperties.Builder builder) {
        String profile = ofNullable(System.getProperty("config.profile"))
                .orElseGet(() -> ofNullable(System.getenv("CONFIG_PROFILE"))
                        .orElse(alias()) // default
                );
        String preProfileFilename = String.format("%s-application.properties", profile);
        String postProfileFilename = String.format("application-%s.properties", profile);
        {
            builder.classpathPropertiesFile("application.properties");
            builder.classpathPropertiesFile(alias() + "/application.properties");
            builder.classpathPropertiesFile(preProfileFilename);
            builder.classpathPropertiesFile(postProfileFilename);
            builder.filesystemPropertiesFile(preProfileFilename);
            builder.filesystemPropertiesFile(postProfileFilename);
            builder.filesystemPropertiesFile("gitignore/application.properties");
            builder.filesystemPropertiesFile("gitignore/" + preProfileFilename);
            builder.filesystemPropertiesFile("gitignore/" + postProfileFilename);
            builder.filesystemPropertiesFile("gitignore/" + profile + ".properties");
            builder.enableSystemProperties();
            builder.enableEnvironmentVariables();
        }
        return builder;
    }
}
