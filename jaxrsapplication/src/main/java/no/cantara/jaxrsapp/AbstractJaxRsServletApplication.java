package no.cantara.jaxrsapp;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.jersey3.MetricsFeature;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.ClassLoadingGaugeSet;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.JvmAttributeGaugeSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import io.dropwizard.metrics.jetty11.InstrumentedConnectionFactory;
import io.dropwizard.metrics.jetty11.InstrumentedHttpChannelListener;
import io.dropwizard.metrics.jetty11.InstrumentedQueuedThreadPool;
import io.dropwizard.metrics.servlets.AdminServlet;
import io.dropwizard.metrics.servlets.HealthCheckServlet;
import io.dropwizard.metrics.servlets.MetricsServlet;
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
import no.cantara.jaxrsapp.metrics.FileDescriptorMetricSet;
import no.cantara.jaxrsapp.security.SecurityFilter;
import no.cantara.security.authentication.AuthenticationManager;
import no.cantara.security.authentication.AuthenticationManagerFactory;
import no.cantara.security.authorization.AccessManager;
import no.cantara.security.authorization.AccessManagerFactory;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.internal.routing.RoutingContext;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.URL;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.EventListener;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public abstract class AbstractJaxRsServletApplication<A extends AbstractJaxRsServletApplication<A>> implements JaxRsServletApplication<A>, JaxRsRegistry {

    private static final Logger log = LoggerFactory.getLogger(AbstractJaxRsServletApplication.class);

    protected final String applicationAlias;
    protected final ApplicationProperties config;
    protected final ResourceConfig resourceConfig; // jakarta.ws.rs.core.Application
    protected final AtomicReference<Server> jettyServerRef = new AtomicReference<>();

    protected final List<FilterSpec> filterSpecs = new CopyOnWriteArrayList<>();
    protected final Map<String, Object> singletonByType = new ConcurrentHashMap<>();
    protected final Map<String, Supplier<Object>> initOverrides = new ConcurrentHashMap<>();
    protected final AtomicBoolean initialized = new AtomicBoolean(false);

    protected AbstractJaxRsServletApplication(String applicationAlias, ApplicationProperties config) {
        this.applicationAlias = applicationAlias;
        this.config = config;
        put(ApplicationProperties.class, config);
        resourceConfig = new ResourceConfig();
        put(Application.class, resourceConfig);
        put(ResourceConfig.class, resourceConfig);
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
        resourceConfig.register(instance);
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
            QueuedThreadPool threadPool;
            ConnectionFactory connectionFactory;
            EventListener httpChannelListener = null;
            MetricRegistry jettyMetricRegistry = getOrNull("metrics.jetty");
            if (jettyMetricRegistry != null) {
                // jetty with metrics instrumentation
                threadPool = new InstrumentedQueuedThreadPool(jettyMetricRegistry);
                Timer connectionFactoryTimer = new Timer();
                Counter connectionFactoryCounter = new Counter();
                jettyMetricRegistry.register("connection-factory.connection-duration", connectionFactoryTimer);
                jettyMetricRegistry.register("connection-factory.connections", connectionFactoryCounter);
                connectionFactory = new InstrumentedConnectionFactory(new HttpConnectionFactory(), connectionFactoryTimer, connectionFactoryCounter);
                httpChannelListener = new InstrumentedHttpChannelListener(jettyMetricRegistry, "http-channel");
            } else {
                // plain jetty
                threadPool = new QueuedThreadPool();
                connectionFactory = new HttpConnectionFactory();
            }
            int port = config.asInt("server.port");
            final Server server = new Server(threadPool);
            jettyServerRef.set(server);
            ServerConnector connector = new ServerConnector(server, connectionFactory);
            connector.setPort(port);
            if (httpChannelListener != null) {
                connector.addEventListener(httpChannelListener);
            }
            server.setConnectors(new Connector[]{connector});
            put(Server.class, server);
            ResourceConfig resourceConfig = ResourceConfig.forApplication(this.resourceConfig);
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
            jettyServerRef.get().stop();
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
        String contextPath = normalizeContextPath(config.get("server.context-path"));
        for (FilterSpec filterSpec : filterSpecs) {
            FilterHolder filterHolder = new FilterHolder(filterSpec.filter);
            servletContextHandler.addFilter(filterHolder, filterSpec.pathSpec, filterSpec.dispatches);
        }
        AdminServlet adminServlet = getOrNull(AdminServlet.class);
        if (adminServlet != null) {
            MetricRegistry baseMetricRegistry = getOrNull("metrics.base");
            if (baseMetricRegistry != null) {
                servletContextHandler.getServletContext().setAttribute(MetricsServlet.METRICS_REGISTRY, baseMetricRegistry);
            }
            HealthCheckRegistry healthCheckRegistry = getOrNull(HealthCheckRegistry.class);
            if (healthCheckRegistry != null) {
                servletContextHandler.getServletContext().setAttribute(HealthCheckServlet.HEALTH_CHECK_REGISTRY, healthCheckRegistry);
            }
            servletContextHandler.addServlet(new ServletHolder(adminServlet), "/admin/*");
        }
        MetricRegistry appMetricsRegistry = getOrNull(MetricRegistry.class);
        if (appMetricsRegistry != null) {
            servletContextHandler.addServlet(new ServletHolder(new MetricsServlet(appMetricsRegistry)), "/admin/metrics/app/*");
        }
        MetricRegistry jerseyMetricsRegistry = getOrNull("metrics.jersey");
        if (jerseyMetricsRegistry != null) {
            servletContextHandler.addServlet(new ServletHolder(new MetricsServlet(jerseyMetricsRegistry)), "/admin/metrics/jersey/*");
        }
        MetricRegistry jettyMetricsRegistry = getOrNull("metrics.jetty");
        if (jettyMetricsRegistry != null) {
            servletContextHandler.addServlet(new ServletHolder(new MetricsServlet(jettyMetricsRegistry)), "/admin/metrics/jetty/*");
        }
        MetricRegistry jvmMetricsRegistry = getOrNull("metrics.jvm");
        if (jvmMetricsRegistry != null) {
            servletContextHandler.addServlet(new ServletHolder(new MetricsServlet(jvmMetricsRegistry)), "/admin/metrics/jvm/*");
        }
        ServletContainer jerseyServlet = new ServletContainer(resourceConfig);
        put(ServletContainer.class, jerseyServlet);
        servletContextHandler.addServlet(new ServletHolder(jerseyServlet), contextPath + "/*");
        return servletContextHandler;
    }

    private String normalizeContextPath(String contextPath) {
        Objects.requireNonNull(contextPath);
        String c = contextPath;
        // trim leading slashes
        while (c.startsWith("/")) {
            c = c.substring(1);
        }
        // trim trailing slashes
        while (c.endsWith("/")) {
            c = c.substring(0, c.length() - 1);
        }
        // add single leading slash
        c = "/" + c;
        return c;
    }

    @Override
    public int getBoundPort() {
        int port = ((ServerConnector) jettyServerRef.get().getConnectors()[0]).getLocalPort();
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

    protected MetricRegistry initMetrics() {
        MetricRegistry appMetricRegistry = init(MetricRegistry.class, MetricRegistry::new);
        appMetricRegistry.register("name", (Gauge<String>) this::alias);
        MetricRegistry baseMetricRegistry = init("metrics.base", MetricRegistry::new);
        baseMetricRegistry.register("app", appMetricRegistry);
        return appMetricRegistry;
    }

    protected void initJerseyMetrics() {
        MetricRegistry metricRegistry = get("metrics.base");
        MetricRegistry jerseyMetricRegistry = init("metrics.jersey", MetricRegistry::new);
        metricRegistry.register("jersey", jerseyMetricRegistry);
        initAndRegisterJaxRsWsComponent(MetricsFeature.class, () -> new MetricsFeature(jerseyMetricRegistry));
    }

    protected void initJettyMetrics() {
        MetricRegistry metricRegistry = get("metrics.base");
        MetricRegistry jettyMetricRegistry = init("metrics.jetty", MetricRegistry::new);
        metricRegistry.register("jetty", jettyMetricRegistry);
    }

    protected void initJvmMetrics() {
        MetricRegistry metricRegistry = get("metrics.base");
        MetricRegistry jvmMetricRegistry = init("metrics.jvm", MetricRegistry::new);
        jvmMetricRegistry.registerAll("memory", new MemoryUsageGaugeSet());
        jvmMetricRegistry.registerAll("threads", new ThreadStatesGaugeSet());
        // jvmMetricRegistry.registerAll("threads", new CachedThreadStatesGaugeSet(5, TimeUnit.SECONDS));
        jvmMetricRegistry.registerAll("runtime", new JvmAttributeGaugeSet());
        jvmMetricRegistry.registerAll("gc", new GarbageCollectorMetricSet());
        jvmMetricRegistry.register("fd", new FileDescriptorMetricSet());
        jvmMetricRegistry.register("classes", new ClassLoadingGaugeSet());
        jvmMetricRegistry.registerAll("buffers", new BufferPoolMetricSet(ManagementFactory.getPlatformMBeanServer()));
        metricRegistry.register("jvm", jvmMetricRegistry);
    }

    protected void initHealth(String groupId, String artifactId, HealthProbe... healthProbes) {
        if (getOrNull("version") == null) {
            initVersion(groupId, artifactId);
        }
        initVisualeHealth();
        HealthService healthService = get(HealthService.class);
        for (HealthProbe healthProbe : healthProbes) {
            healthService.registerHealthProbe(healthProbe.getKey(), healthProbe.getProbe());
        }
    }

    protected void initVersion(String groupId, String artifactId) {
        init("version", () -> readMetaInfMavenPomVersion(groupId, artifactId));
    }

    protected void initVersion(String version) {
        init("version", () -> version);
    }

    protected void initVisualeHealth() {
        String version = getOrDefault("version", "UNKNOWN");
        init(HealthCheckRegistry.class, this::createHealthRegistry);
        init(HealthService.class, () -> createHealthService(version));
        initAndRegisterJaxRsWsComponent(HealthResource.class, this::createHealthResource);
    }

    protected HealthCheckRegistry createHealthRegistry() {
        return new HealthCheckRegistry();
    }

    protected HealthService createHealthService(String version) {
        HealthCheckRegistry healthCheckRegistry = get(HealthCheckRegistry.class);
        HealthService healthService = new HealthService(version, healthCheckRegistry, 1800, ChronoUnit.MILLIS);
        return healthService;
    }

    protected HealthResource createHealthResource() {
        HealthService healthService = get(HealthService.class);
        HealthResource healthResource = new HealthResource(healthService);
        return healthResource;
    }

    protected void initAdminServlet() {
        init(AdminServlet.class, AdminServlet::new);
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

    private String readMetaInfMavenPomVersion(String groupId, String artifactId) {
        String resourcePath = String.format("/META-INF/maven/%s/%s/pom.properties", groupId, artifactId);
        URL mavenVersionResource = getClass().getResource(resourcePath);
        if (mavenVersionResource != null) {
            try {
                Properties mavenProperties = new Properties();
                mavenProperties.load(mavenVersionResource.openStream());
                return mavenProperties.getProperty("version", "missing version info in " + resourcePath);
            } catch (IOException e) {
                log.warn("Problem reading version resource from classpath: ", e);
            }
        }
        return "unknown";
    }
}
