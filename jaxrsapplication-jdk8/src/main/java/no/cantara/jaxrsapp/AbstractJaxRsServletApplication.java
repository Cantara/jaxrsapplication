package no.cantara.jaxrsapp;

import no.cantara.config.ApplicationProperties;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Application;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public abstract class AbstractJaxRsServletApplication<A extends AbstractJaxRsServletApplication<A>> implements JaxRsServletApplication<A> {

    private static final Logger log = LoggerFactory.getLogger(AbstractJaxRsServletApplication.class);

    protected final ApplicationProperties config;
    protected final Application application;
    protected final Server server;

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
}
