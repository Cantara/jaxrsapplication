package no.cantara.jaxrsapp.test;

import no.cantara.config.ApplicationProperties;
import no.cantara.config.ProviderLoader;
import no.cantara.jaxrsapp.JaxRsServletApplication;
import no.cantara.jaxrsapp.JaxRsServletApplicationFactory;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import javax.inject.Inject;
import javax.inject.Named;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;

import static java.util.Optional.ofNullable;

public class IntegrationTestExtension implements BeforeEachCallback, BeforeAllCallback, AfterAllCallback {

    Map<String, TestClient> client = new LinkedHashMap<>();
    Map<String, JaxRsServletApplication> applicationByProvider = new LinkedHashMap<>();

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        Class<?> testClass = extensionContext.getRequiredTestClass();

        JaxRsApplicationProvider jaxRsApplicationProvider = testClass.getDeclaredAnnotation(JaxRsApplicationProvider.class);

        if (jaxRsApplicationProvider == null || jaxRsApplicationProvider.value().length <= 1) {

            /*
             * Single application
             */

            String providerAlias = null;
            if (jaxRsApplicationProvider == null || jaxRsApplicationProvider.value().length == 0) {
                // pick first available application
                ServiceLoader<JaxRsServletApplicationFactory> loader = ServiceLoader.load(JaxRsServletApplicationFactory.class);
                for (JaxRsServletApplicationFactory factory : loader) {
                    providerAlias = factory.alias();
                    break;
                }
                if (providerAlias == null) {
                    throw new IllegalArgumentException("Error, Application need to implement the interface " + JaxRsServletApplicationFactory.class.getName());
                }
            } else {
                // guaranteed (jaxRsApplicationProvider.value().length == 1)
                providerAlias = jaxRsApplicationProvider.value()[0];
            }

            ApplicationProperties.Builder configBuilder = ApplicationProperties.builder()
                    .classpathPropertiesFile(providerAlias + "/application.properties")
                    .testDefaults();
            initTestApplication(testClass, providerAlias, configBuilder);

        } else {

            /*
             * Multi application
             */

            for (String providerAlias : jaxRsApplicationProvider.value()) {
                ApplicationProperties.Builder configBuilder = ApplicationProperties.builder()
                        .classpathPropertiesFile(providerAlias + "/application.properties")
                        .testDefaults();
                initTestApplication(testClass, providerAlias, configBuilder);
            }
        }
    }

    private void initTestApplication(Class<?> testClass, String providerAlias, ApplicationProperties.Builder configBuilder) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        ConfigOverride configOverride = testClass.getDeclaredAnnotation(ConfigOverride.class);
        String profile = ofNullable(System.getProperty("config.profile"))
                .orElseGet(() -> ofNullable(System.getenv("CONFIG_PROFILE"))
                        .orElse(providerAlias) // default
                );
        if (profile != null) {
                String preProfileFilename = String.format("%s-application.properties", profile);
                String postProfileFilename = String.format("application-%s.properties", profile);
                configBuilder.classpathPropertiesFile(preProfileFilename);
                configBuilder.classpathPropertiesFile(postProfileFilename);
                configBuilder.filesystemPropertiesFile(preProfileFilename);
                configBuilder.filesystemPropertiesFile(postProfileFilename);
        }
        String overrideFile = ofNullable(System.getProperty("config.file"))
                .orElseGet(() -> System.getenv("CONFIG_FILE"));
        if (overrideFile != null) {
            configBuilder.filesystemPropertiesFile(overrideFile);
        }
        if (configOverride != null) {
            String[] overrideArray = configOverride.value();
            Map<String, String> configOverrideMap = new LinkedHashMap<>();
            for (int i = 0; i < overrideArray.length; i += 2) {
                configOverrideMap.put(overrideArray[i], overrideArray[i + 1]);
            }
            configBuilder.map(configOverrideMap);
        }
        ApplicationProperties config = configBuilder.build();

        JaxRsServletApplication application = (JaxRsServletApplication) ProviderLoader.configure(config, providerAlias, JaxRsServletApplicationFactory.class);
        applicationByProvider.put(providerAlias, application);
        application.override(ApplicationProperties.class, () -> config);

        MockRegistryConfig applicationConfig = testClass.getDeclaredAnnotation(MockRegistryConfig.class);
        if (applicationConfig != null) {
            Class<? extends MockRegistry> registryClazz = applicationConfig.value();
            Constructor<?> constructor = registryClazz.getDeclaredConstructors()[0];
            MockRegistry mockRegistry = (MockRegistry) constructor.newInstance();
            for (Class<?> mockClazz : mockRegistry) {
                Object instance = mockRegistry.get(mockClazz);
                application.override(mockClazz, () -> instance);
            }
        }

        application.init();
        application.start();

        int boundPort = application.getBoundPort();
        client.put(providerAlias, TestClient.newClient("localhost", boundPort));
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) {
        Object test = extensionContext.getRequiredTestInstance();
        Field[] fields = test.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (!field.isAnnotationPresent(Inject.class)) {
                continue;
            }
            String fieldNamed = null;
            if (field.isAnnotationPresent(Named.class)) {
                Named named = field.getDeclaredAnnotation(Named.class);
                fieldNamed = named.value();
            }
            // application
            if (JaxRsServletApplication.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    if (field.get(test) == null) {
                        if (fieldNamed != null) {
                            field.set(test, applicationByProvider.get(fieldNamed));
                        } else {
                            field.set(test, applicationByProvider.values().iterator().next());
                        }
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
            if (TestClient.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    if (field.get(test) == null) {
                        if (fieldNamed != null) {
                            field.set(test, client.get(fieldNamed));
                        } else {
                            field.set(test, client.values().iterator().next());
                        }
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) {
        for (Map.Entry<String, JaxRsServletApplication> entry : applicationByProvider.entrySet()) {
            JaxRsServletApplication application = entry.getValue();
            application.stop();
        }
    }
}
