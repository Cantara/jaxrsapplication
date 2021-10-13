package no.cantara.jaxrsapp.test;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import no.cantara.config.ApplicationProperties;
import no.cantara.config.ProviderLoader;
import no.cantara.jaxrsapp.JaxRsServletApplication;
import no.cantara.jaxrsapp.JaxRsServletApplicationFactory;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

            ApplicationProperties.Builder configBuilder = resolveConfiguration(testClass, providerAlias);
            ApplicationProperties config = configBuilder.build();
            initTestApplication(testClass, providerAlias, config);

        } else {

            /*
             * Multi application
             */

            Map<String, ApplicationProperties.Builder> configBuilderByAlias = resolveAllConfigurations(testClass, jaxRsApplicationProvider.value());

            Node root = buildDependencyGraph(testClass, configBuilderByAlias, jaxRsApplicationProvider.value());

            Set<Node> ancestors = new LinkedHashSet<>();
            NodeTraversals.depthFirstPostOrder(ancestors, root, node -> {
                if (node == root) {
                    return;
                }
                String providerAlias = node.getId();
                ApplicationProperties.Builder configBuilder = configBuilderByAlias.get(providerAlias);

                /*
                 * Initialize application
                 */
                JaxRsServletApplication application = initTestApplication(testClass, providerAlias, configBuilder.build());

                if (node.getParents().get(0).equals(root)) {
                    return; // only meta-node root depends on this application
                }
                for (Node parent : node.getParents()) {
                    String parentConfigKey = parent.getAttribute("${" + providerAlias + ".port}");
                    ApplicationProperties.Builder parentConfigBuilder = configBuilderByAlias.get(parent.getId());

                    /*
                     * Override expression with value of bound port
                     */
                    parentConfigBuilder.values().put(parentConfigKey, application.getBoundPort()).end();
                }
            });
        }
    }

    private Node buildDependencyGraph(Class<?> testClass, Map<String, ApplicationProperties.Builder> configBuilderByAlias, String[] providerAliases) {
        final Node root = new Node(null, "root");
        final Map<String, Node> nodeById = new LinkedHashMap<>();
        final Set<String> patterns = new LinkedHashSet<>();
        final List<Node> nodes = new LinkedList<>();
        for (String providerAlias : providerAliases) {
            Node node = new Node(root, providerAlias);
            nodeById.put(providerAlias, node);
            root.addChild(node);
            patterns.add("${" + providerAlias + ".port}");
            nodes.add(node);
        }

        // Build dependency graph
        final Pattern pattern = Pattern.compile("\\$\\{([^.]+)[.]port}");
        for (Node node : nodes) {
            ApplicationProperties.Builder configBuilder = resolveConfiguration(testClass, node.getId());
            configBuilderByAlias.put(node.getId(), configBuilder);
            ApplicationProperties config = configBuilder.build();
            for (Map.Entry<String, String> entry : config.map().entrySet()) {
                String value = entry.getValue();
                if (patterns.contains(value)) {
                    Matcher m = pattern.matcher(value);
                    if (!m.matches()) {
                        throw new RuntimeException("Pattern does not match, wi");
                    }
                    node.putAttribute(value, entry.getKey());
                    String dependOnApplication = m.group(1);
                    Node dependOnNode = nodeById.get(dependOnApplication);
                    if (dependOnNode == null) {
                        throw new RuntimeException("Configuration expression points to an application that is not configured. Expression: " + value);
                    }
                    node.addChild(dependOnNode);
                    dependOnNode.getParents().add(node);
                    dependOnNode.getParents().remove(root);
                    root.getChildren().remove(dependOnNode);
                }
            }
        }
        return root;
    }

    private Map<String, ApplicationProperties.Builder> resolveAllConfigurations(Class<?> testClass, String[] providerAliases) {
        Map<String, ApplicationProperties.Builder> configByAlias = new LinkedHashMap<>();
        for (String providerAlias : providerAliases) {
            ApplicationProperties.Builder configBuilder = resolveConfiguration(testClass, providerAlias);
            configByAlias.put(providerAlias, configBuilder);
        }
        return configByAlias;
    }

    private JaxRsServletApplication initTestApplication(Class<?> testClass, String providerAlias, ApplicationProperties config) {
        JaxRsServletApplication application = (JaxRsServletApplication) ProviderLoader.configure(config, providerAlias, JaxRsServletApplicationFactory.class);
        applicationByProvider.put(providerAlias, application);
        application.override(ApplicationProperties.class, () -> config);

        MockRegistryConfig applicationConfig = testClass.getDeclaredAnnotation(MockRegistryConfig.class);
        if (applicationConfig != null) {
            Class<? extends MockRegistry> registryClazz = applicationConfig.value();
            Constructor<?> constructor = registryClazz.getDeclaredConstructors()[0];
            MockRegistry mockRegistry;
            try {
                mockRegistry = (MockRegistry) constructor.newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            for (Class<?> mockClazz : mockRegistry) {
                Object instance = mockRegistry.get(mockClazz);
                application.override(mockClazz, () -> instance);
            }
        }

        application.init();
        application.start();

        int boundPort = application.getBoundPort();
        client.put(providerAlias, TestClient.newClient("localhost", boundPort));

        return application;
    }

    private ApplicationProperties.Builder resolveConfiguration(Class<?> testClass, String providerAlias) {
        ApplicationProperties.Builder configBuilder = ApplicationProperties.builder()
                .classpathPropertiesFile(providerAlias + "/application.properties")
                .testDefaults();
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
        ConfigOverride configOverride = testClass.getDeclaredAnnotation(ConfigOverride.class);
        if (configOverride != null) {
            String[] overrideArray = configOverride.value();
            Map<String, String> configOverrideMap = new LinkedHashMap<>();
            for (int i = 0; i < overrideArray.length; i += 2) {
                configOverrideMap.put(overrideArray[i], overrideArray[i + 1]);
            }
            configBuilder.map(configOverrideMap);
        }
        return configBuilder;
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
