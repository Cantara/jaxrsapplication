package no.cantara.jaxrsapp;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Request;
import no.cantara.config.ApplicationProperties;
import no.cantara.config.ProviderLoader;
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
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

public abstract class AbstractJaxRsServletApplication<A extends AbstractJaxRsServletApplication<A>> implements JaxRsServletApplication<A> {

    private static final Logger log = LoggerFactory.getLogger(AbstractJaxRsServletApplication.class);

    protected final ApplicationProperties config;
    protected final Application application;
    protected final Server server;

    protected final List<FilterSpec> filterSpecs = new CopyOnWriteArrayList<>();
    protected final Map<Class<?>, Object> jaxRsWsComponentByType = new ConcurrentHashMap<>();
    protected final Map<Class<?>, Object> singletonByType = new ConcurrentHashMap<>();
    protected final Map<Class<?>, Supplier<Object>> initOverrides = new ConcurrentHashMap<>();

    protected AbstractJaxRsServletApplication(ApplicationProperties config) {
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
    public A override(Class<?> clazz, Supplier<Object> init) {
        initOverrides.put(clazz, init);
        return (A) this;
    }

    @Override
    public A put(Class<?> clazz, Object instance) {
        this.singletonByType.put(clazz, instance);
        return (A) this;
    }

    @Override
    public <T> T get(Class<T> clazz) {
        return (T) this.singletonByType.get(clazz);
    }

    protected <T extends Filter> T initAndAddServletFilter(Class<T> clazz, Supplier<T> filterSupplier, String pathSpec, EnumSet<DispatcherType> dispatches) {
        T instance = init(clazz, filterSupplier);
        filterSpecs.add(new FilterSpec(clazz, instance, pathSpec, dispatches));
        return instance;
    }

    protected <T> T initAndRegisterJaxRsWsComponent(Class<T> clazz, Supplier<T> init) {
        T instance = init(clazz, init);
        jaxRsWsComponentByType.put(clazz, instance);
        return instance;
    }

    protected <T> T init(Class<T> clazz, Supplier<T> init) {
        Supplier<T> initOverride = (Supplier<T>) initOverrides.get(clazz);
        T instance;
        if (initOverride != null) {
            instance = initOverride.get();
        } else {
            instance = init.get();
        }
        put(clazz, instance);
        return instance;
    }

    @Override
    public A start() {
        try {
            ResourceConfig resourceConfig = ResourceConfig.forApplication(application);
            ServletContextHandler servletContextHandler = createServletContextHandler(resourceConfig);
            put(ServletContextHandler.class, servletContextHandler);
            server.setHandler(servletContextHandler);
            server.start();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return (A) this;
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
        protected final Class<? extends Filter> clazz;
        protected final Filter filter;
        protected final String pathSpec;
        protected final EnumSet<DispatcherType> dispatches;

        public FilterSpec(Class<? extends Filter> clazz, Filter filter, String pathSpec, EnumSet<DispatcherType> dispatches) {
            this.clazz = clazz;
            this.filter = filter;
            this.pathSpec = pathSpec;
            this.dispatches = dispatches;
        }
    }

    protected void initSecurity() {
        init(AuthenticationManager.class, this::initAuthenticationManager);
        init(AccessManager.class, this::initAccessManager);
        initAndRegisterJaxRsWsComponent(SecurityFilter.class, this::initSecurityFilter);
    }

    protected AuthenticationManager initAuthenticationManager() {
        String provider = config.get("authentication.provider", "default");
        AuthenticationManager authenticationManager = ProviderLoader.configure(config, provider, AuthenticationManagerFactory.class);
        return authenticationManager;
    }

    protected AccessManager initAccessManager() {
        ApplicationProperties authConfig = ApplicationProperties.builder()
                .classpathPropertiesFile("service-authorization.properties")
                .classpathPropertiesFile("authorization.properties")
                .filesystemPropertiesFile("authorization.properties")
                .build();
        String provider = config.get("authorization.provider", "default");
        AccessManager accessManager = ProviderLoader.configure(authConfig, provider, AccessManagerFactory.class);
        return accessManager;
    }

    protected SecurityFilter initSecurityFilter() {
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
