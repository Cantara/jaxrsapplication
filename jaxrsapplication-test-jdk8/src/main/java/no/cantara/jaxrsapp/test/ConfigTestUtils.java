package no.cantara.jaxrsapp.test;

import no.cantara.config.ApplicationProperties;

import static java.util.Optional.ofNullable;

public class ConfigTestUtils {

    public static ApplicationProperties.Builder conventions(ApplicationProperties.Builder builder, String applicationAlias) {
        builder.classpathPropertiesFile("application.properties");
        builder.classpathPropertiesFile(applicationAlias + "/application.properties");
        {
            String preProfileFilename = String.format("%s-application.properties", applicationAlias);
            String postProfileFilename = String.format("application-%s.properties", applicationAlias);
            builder.classpathPropertiesFile(preProfileFilename);
            builder.classpathPropertiesFile(postProfileFilename);
            builder.filesystemPropertiesFile(preProfileFilename);
            builder.filesystemPropertiesFile(postProfileFilename);
            builder.filesystemPropertiesFile("gitignore/application.properties");
            builder.filesystemPropertiesFile("gitignore/" + preProfileFilename);
            builder.filesystemPropertiesFile("gitignore/" + postProfileFilename);
            builder.filesystemPropertiesFile("gitignore/" + applicationAlias + ".properties");
        }
        {
            builder.classpathPropertiesFile("test_override.properties");
            String preProfileFilename = String.format("%s-test.properties", applicationAlias);
            String postProfileFilename = String.format("test-%s.properties", applicationAlias);
            builder.classpathPropertiesFile(preProfileFilename);
            builder.classpathPropertiesFile(postProfileFilename);
            builder.classpathPropertiesFile(applicationAlias + "/" + preProfileFilename);
            builder.classpathPropertiesFile(applicationAlias + "/" + postProfileFilename);
            builder.classpathPropertiesFile(applicationAlias + "/test_override.properties");
            builder.filesystemPropertiesFile("gitignore/test_override.properties");
            builder.filesystemPropertiesFile("gitignore/" + preProfileFilename);
            builder.filesystemPropertiesFile("gitignore/" + postProfileFilename);
            builder.filesystemPropertiesFile("gitignore/" + applicationAlias + ".properties");
        }
        String overrideFile = ofNullable(System.getProperty("config.file"))
                .orElseGet(() -> System.getenv("CONFIG_FILE"));
        if (overrideFile != null) {
            builder.filesystemPropertiesFile(overrideFile);
        }
        return builder;
    }
}
