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

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Optional.ofNullable;

public class IntegrationTestExtension implements BeforeEachCallback, BeforeAllCallback, AfterAllCallback {

    final Map<String, JaxRsServletApplication> applicationByAlias = new LinkedHashMap<>();
    final Map<String, TestClient> clientByAlias = new LinkedHashMap<>();
    final Set<String> applicationAliases = new LinkedHashSet<>();
    final AtomicInteger nextAppInitThreadId = new AtomicInteger(1);
    final ExecutorService appInitExecutor = Executors.newCachedThreadPool(runnable -> new Thread(runnable, "app-init-" + nextAppInitThreadId.getAndIncrement()));

    Node root;

    @Override
    public void beforeAll(ExtensionContext context) {
        Class<?> testClass = context.getRequiredTestClass();

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
                    throw new IllegalArgumentException("Error, Provide a service-provider in META-INF/services folder that implements the interface " + JaxRsServletApplicationFactory.class.getName());
                }
            } else {
                // guaranteed (jaxRsApplicationProvider.value().length == 1)
                providerAlias = jaxRsApplicationProvider.value()[0];
            }

            applicationAliases.add(providerAlias);

        } else {

            /*
             * Multi application
             */

            for (String providerAlias : jaxRsApplicationProvider.value()) {
                applicationAliases.add(providerAlias);
            }
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        Class<?> testClass = context.getRequiredTestClass();
        Object testInstance = context.getRequiredTestInstance();

        if (root == null) {
            root = buildDependencyGraph(testClass, applicationAliases);
            Set<Node> ancestors = new LinkedHashSet<>();
            NodeTraversals.depthFirstPostOrderWithConcurrentSiblingExecutions(appInitExecutor, ancestors, root, node -> {
                if (node == root) {
                    return;
                }

                String providerAlias = node.getId();
                ApplicationProperties.Builder configBuilder = node.get("config-builder");
                configBuilder.enableSystemProperties()
                        .enableEnvironmentVariables();

                if (testInstance instanceof BeforeCreationLifecycleListener) {
                    BeforeCreationLifecycleListener lifecycleListener = (BeforeCreationLifecycleListener) testInstance;
                    lifecycleListener.beforeCreation(configBuilder);
                }

                ApplicationProperties config = configBuilder.build();

                /*
                 * Initialize application
                 */
                JaxRsServletApplication application = (JaxRsServletApplication) ProviderLoader.configure(config, providerAlias, JaxRsServletApplicationFactory.class);
                applicationByAlias.put(providerAlias, application);
                initTestApplication(application, testInstance, providerAlias);

                if (node.getParents().get(0).equals(root)) {
                    return; // only meta-node root depends on this application
                }
                for (Node parent : node.getParents()) {
                    String parentConfigKey = parent.getAttribute("${" + providerAlias + ".port}");
                    ApplicationProperties.Builder parentConfigBuilder = parent.get("config-builder");

                    /*
                     * Override expression with value of bound port
                     */
                    parentConfigBuilder.values().put(parentConfigKey, application.getBoundPort()).end();
                }
            });
        }

        for (Map.Entry<String, JaxRsServletApplication> entry : applicationByAlias.entrySet()) {
            String key = entry.getKey();
            JaxRsServletApplication application = entry.getValue();
            if (!application.isInitialized()) {
                ApplicationProperties config = application.get(ApplicationProperties.class);
            }
        }

        Object test = context.getRequiredTestInstance();
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
                            field.set(test, applicationByAlias.get(fieldNamed));
                        } else {
                            field.set(test, applicationByAlias.values().iterator().next());
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
                            field.set(test, clientByAlias.get(fieldNamed));
                        } else {
                            field.set(test, clientByAlias.values().iterator().next());
                        }
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private Node buildDependencyGraph(Class<?> testClass, Set<String> providerAliases) {
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
            node.put("config-builder", configBuilder);
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

    private ApplicationProperties.Builder resolveConfiguration(Class<?> testClass, String providerAlias) {
        String profile = ofNullable(System.getProperty("config.profile"))
                .orElseGet(() -> ofNullable(System.getenv("CONFIG_PROFILE"))
                        .orElse(providerAlias) // default
                );
        ApplicationProperties.Builder configBuilder = ApplicationProperties.builder();
        ConfigTestUtils.conventions(configBuilder, profile);
        {
            ConfigOverride configOverride = testClass.getDeclaredAnnotation(ConfigOverride.class);
            overrideConfig(configBuilder, configOverride, providerAlias);
        }
        ConfigOverrides configOverrides = testClass.getDeclaredAnnotation(ConfigOverrides.class);
        if (configOverrides != null) {
            for (ConfigOverride configOverride : configOverrides.value()) {
                overrideConfig(configBuilder, configOverride, providerAlias);
            }
        }
        return configBuilder;
    }

    private void overrideConfig(ApplicationProperties.Builder configBuilder, ConfigOverride configOverride, String providerAlias) {
        if (configOverride == null) {
            return;
        }
        if (!configOverride.application().isEmpty() && !configOverride.application().equals(providerAlias)) {
            return; // configuration override not mapped to this provider-alias
        }
        String[] overrideArray = configOverride.value();
        Map<String, String> configOverrideMap = new LinkedHashMap<>();
        for (int i = 0; i < overrideArray.length; i += 2) {
            configOverrideMap.put(overrideArray[i], overrideArray[i + 1]);
        }
        configBuilder.map(configOverrideMap);
    }

    private JaxRsServletApplication initTestApplication(JaxRsServletApplication application, Object testInstance, String providerAlias) {

        if (testInstance instanceof BeforeInitLifecycleListener) {
            BeforeInitLifecycleListener lifecycleListener = (BeforeInitLifecycleListener) testInstance;
            lifecycleListener.beforeInit(application);
        }

        application.init();

        if (testInstance instanceof AfterInitLifecycleListener) {
            AfterInitLifecycleListener lifecycleListener = (AfterInitLifecycleListener) testInstance;
            lifecycleListener.afterInit(application);
        }

        application.start(false);

        if (testInstance instanceof AfterStartLifecycleListener) {
            AfterStartLifecycleListener lifecycleListener = (AfterStartLifecycleListener) testInstance;
            lifecycleListener.afterStart(application);
        }

        application.postInit();

        if (testInstance instanceof AfterPostInitLifecycleListener) {
            AfterPostInitLifecycleListener lifecycleListener = (AfterPostInitLifecycleListener) testInstance;
            lifecycleListener.afterPostInit(application);
        }

        int boundPort = application.getBoundPort();
        clientByAlias.put(providerAlias, DefaultTestClient.newClient("http", "localhost", boundPort));

        return application;
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) {
        for (Map.Entry<String, JaxRsServletApplication> entry : applicationByAlias.entrySet()) {
            JaxRsServletApplication application = entry.getValue();
            application.stop();
        }
    }
}
