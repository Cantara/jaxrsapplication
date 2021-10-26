package no.cantara.jaxrsapp;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Request;
import no.cantara.config.ApplicationProperties;
import no.cantara.config.ProviderLoader;
import no.cantara.jaxrsapp.health.HealthProbe;
import no.cantara.jaxrsapp.health.HealthResource;
import no.cantara.jaxrsapp.health.HealthService;
import no.cantara.jaxrsapp.security.SecurityFilter;
import no.cantara.security.authentication.AuthenticationManager;
import no.cantara.security.authentication.AuthenticationManagerFactory;
import no.cantara.security.authorization.AccessManager;
import no.cantara.security.authorization.AccessManagerFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.internal.routing.RoutingContext;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public abstract class AbstractJaxRsServletApplication<A extends AbstractJaxRsServletApplication<A>> implements JaxRsServletApplication<A>, JaxRsRegistry {

    private static final Logger log = LoggerFactory.getLogger(AbstractJaxRsServletApplication.class);

    protected final String applicationAlias;
    protected final ApplicationProperties config;
    protected final Application application;
    protected final Server server;

    protected final List<FilterSpec> filterSpecs = new CopyOnWriteArrayList<>();
    protected final Map<String, Object> jaxRsWsComponentByType = new ConcurrentHashMap<>();
    protected final Map<String, Object> singletonByType = new ConcurrentHashMap<>();
    protected final Map<String, Supplier<Object>> initOverrides = new ConcurrentHashMap<>();
    protected final AtomicBoolean initialized = new AtomicBoolean(false);

    protected AbstractJaxRsServletApplication(String applicationAlias, ApplicationProperties config) {
        this.applicationAlias = applicationAlias;
        this.config = config;
        put(ApplicationProperties.class, config);
        int port = config.asInt("server.port");
        server = new Server(port);
        put(Server.class, server);
        application = new Application() {
            @Override
            public Set<Object> getSingletons() {
                return new LinkedHashSet<>(jaxRsWsComponentByType.values());
            }
        };
        put(Application.class, application);
    }

    @Override
    public String alias() {
        return applicationAlias;
    }

    @Override
    public ApplicationProperties config() {
        return config;
    }

    @Override
    public final A init() {
        doInit();
        initialized.set(true);
        return (A) this;
    }

    protected abstract void doInit();

    @Override
    public boolean isInitialized() {
        return initialized.get();
    }

    @Override
    public A override(String key, Supplier<Object> init) {
        initOverrides.put(key, init);
        return (A) this;
    }

    @Override
    public <T> JaxRsRegistry put(String key, T instance) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(instance);
        this.singletonByType.put(key, instance);
        return this;
    }

    @Override
    public <T> T get(String key) {
        T instance = (T) this.singletonByType.get(key);
        if (instance == null) {
            throw new NoSuchElementException(key);
        }
        return instance;
    }

    @Override
    public <T> T getOrNull(String key) {
        T instance = (T) this.singletonByType.get(key);
        return instance;
    }

    protected <T extends Filter> T initAndAddServletFilter(Class<T> clazz, Supplier<T> filterSupplier, String pathSpec, EnumSet<DispatcherType> dispatches) {
        return initAndAddServletFilter(clazz.getName(), filterSupplier, pathSpec, dispatches);
    }

    protected <T extends Filter> T initAndAddServletFilter(String key, Supplier<T> filterSupplier, String pathSpec, EnumSet<DispatcherType> dispatches) {
        T instance = init(key, filterSupplier);
        filterSpecs.add(new FilterSpec(key, instance, pathSpec, dispatches));
        return instance;
    }

    protected <T> T initAndRegisterJaxRsWsComponent(Class<T> clazz, Supplier<T> init) {
        return initAndRegisterJaxRsWsComponent(clazz.getName(), init);
    }

    protected <T> T initAndRegisterJaxRsWsComponent(String key, Supplier<T> init) {
        T instance = init(key, init);
        jaxRsWsComponentByType.put(key, instance);
        return instance;
    }

    protected <T> T init(Class<T> clazz, Supplier<T> init) {
        return init(clazz.getName(), init);
    }

    protected <T> T init(String key, Supplier<T> init) {
        Supplier<T> initOverride = (Supplier<T>) initOverrides.get(key);
        T instance;
        if (initOverride != null) {
            instance = initOverride.get();
        } else {
            instance = init.get();
        }
        put(key, instance);
        return instance;
    }

    public A doStart() {
        try {
            ResourceConfig resourceConfig = ResourceConfig.forApplication(application);
            ServletContextHandler servletContextHandler = createServletContextHandler(resourceConfig);
            put(ServletContextHandler.class, servletContextHandler);
            server.setHandler(servletContextHandler);
            server.start();
            return (A) this;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public final A start(boolean alsoRunPostInitAfterStart) {
        doStart();
        if (alsoRunPostInitAfterStart) {
            postInit();
        }
        return (A) this;
    }

    public final A start() {
        return start(true);
    }

    @Override
    public final A postInit() {
        doPostInit();
        return (A) this;
    }

    protected void doPostInit() {
    }

    @Override
    public A stop() {
        try {
            server.stop();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return (A) this;
    }

    protected ServletContextHandler createServletContextHandler(ResourceConfig resourceConfig) {
        Objects.requireNonNull(resourceConfig);
        ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        String contextPath = config.get("server.context-path");
        servletContextHandler.setContextPath(contextPath);
        for (FilterSpec filterSpec : filterSpecs) {
            FilterHolder filterHolder = new FilterHolder(filterSpec.filter);
            servletContextHandler.addFilter(filterHolder, filterSpec.pathSpec, filterSpec.dispatches);
        }
        ServletContainer jerseyServlet = new ServletContainer(resourceConfig);
        put(ServletContainer.class, jerseyServlet);
        servletContextHandler.addServlet(new ServletHolder(jerseyServlet), "/*");
        return servletContextHandler;
    }

    @Override
    public int getBoundPort() {
        int port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
        return port;
    }

    protected static class FilterSpec {
        protected final String key;
        protected final Filter filter;
        protected final String pathSpec;
        protected final EnumSet<DispatcherType> dispatches;

        public FilterSpec(String key, Filter filter, String pathSpec, EnumSet<DispatcherType> dispatches) {
            this.key = key;
            this.filter = filter;
            this.pathSpec = pathSpec;
            this.dispatches = dispatches;
        }
    }

    protected void initHealth(String groupId, String artifactId, HealthProbe... healthProbes) {
        HealthService healthService = init(HealthService.class, () -> createHealthService(groupId, artifactId));
        for (HealthProbe healthProbe : healthProbes) {
            healthService.registerHealthProbe(healthProbe.getKey(), healthProbe.getProbe());
        }
        HealthResource healthResource = initAndRegisterJaxRsWsComponent(HealthResource.class, this::createHealthResource);
    }

    protected HealthService createHealthService(String groupId, String artifactId) {
        HealthService healthService = new HealthService(groupId, groupId, 1800, ChronoUnit.MILLIS);
        return healthService;
    }

    protected HealthResource createHealthResource() {
        HealthService healthService = get(HealthService.class);
        HealthResource healthResource = new HealthResource(healthService);
        return healthResource;
    }

    protected void initSecurity() {
        init(AuthenticationManager.class, this::createAuthenticationManager);
        init(AccessManager.class, this::createAccessManager);
        initAndRegisterJaxRsWsComponent(SecurityFilter.class, this::createSecurityFilter);
    }

    protected AuthenticationManager createAuthenticationManager() {
        String provider = config.get("authentication.provider", "default");
        AuthenticationManager authenticationManager = ProviderLoader.configure(config, provider, AuthenticationManagerFactory.class);
        return authenticationManager;
    }

    protected AccessManager createAccessManager() {
        ApplicationProperties authConfig = ApplicationProperties.builder()
                .classpathPropertiesFile(applicationAlias + "/service-authorization.properties")
                .classpathPropertiesFile(applicationAlias + "/authorization.properties")
                .classpathPropertiesFile(applicationAlias + "-authorization.properties")
                .classpathPropertiesFile("authorization-" + applicationAlias + ".properties")
                .filesystemPropertiesFile("authorization.properties")
                .filesystemPropertiesFile(applicationAlias + "-authorization.properties")
                .filesystemPropertiesFile("authorization-" + applicationAlias + ".properties")
                .build();
        String provider = config.get("authorization.provider", "default");
        AccessManager accessManager = ProviderLoader.configure(authConfig, provider, AccessManagerFactory.class);
        return accessManager;
    }

    protected SecurityFilter createSecurityFilter() {
        AuthenticationManager authenticationManager = get(AuthenticationManager.class);
        AccessManager accessManager = get(AccessManager.class);
        return new SecurityFilter(authenticationManager, accessManager, this::getJaxRsRoutingEndpoint);
    }

    protected Method getJaxRsRoutingEndpoint(ContainerRequestContext requestContext) {
        Request request = requestContext.getRequest();
        ContainerRequest containerRequest = (ContainerRequest) request;
        RoutingContext routingContext = (RoutingContext) containerRequest.getUriInfo();
        Method resourceMethod = routingContext.getResourceMethod();
        return resourceMethod;
    }
}
