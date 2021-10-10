package no.cantara.jaxrsapp;

import no.cantara.config.ApplicationProperties;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public abstract class AbstractJaxRsServletApplication<A extends AbstractJaxRsServletApplication<A>> implements JaxRsServletApplication<A> {

    private static final Logger log = LoggerFactory.getLogger(AbstractJaxRsServletApplication.class);

    protected final ApplicationProperties config;
    protected final Server server;

    private final Set<Object> jaxRsResources = new LinkedHashSet<>();

    private final Map<Class<?>, Object> singletonByType = new ConcurrentHashMap<>();
    private final Map<Class<?>, Supplier<Object>> initOverrides = new ConcurrentHashMap<>();

    public AbstractJaxRsServletApplication(ApplicationProperties config) {
        this.config = config;
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        String contextPath = config.get("server.context-path");
        context.setContextPath(contextPath);
        context.addServlet(new ServletHolder(createJerseyServlet()), "/*");
        int port = config.asInt("server.port");
        server = new Server(port);
        server.setHandler(context);
        put(Server.class, server);
    }

    @Override
    public A override(Class<?> clazz, Supplier<Object> init) {
        initOverrides.put(clazz, init);
        return (A) this;
    }

    @Override
    public A put(Class<?> clazz, Object instance) {
        this.singletonByType.put(clazz, instance);
        Path jaxRsPath = clazz.getDeclaredAnnotation(Path.class);
        if (jaxRsPath != null) {
            jaxRsResources.add(instance);
        }
        return (A) this;
    }

    @Override
    public <T> T get(Class<T> clazz) {
        return (T) this.singletonByType.get(clazz);
    }

    protected <T> T initOrOverride(Class<T> clazz, Supplier<T> init) {
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

    private Servlet createJerseyServlet() {
        ServletContainer servletContainer = new ServletContainer(ResourceConfig.forApplication(new Application() {
            @Override
            public Set<Class<?>> getClasses() {
                return super.getClasses();
            }

            @Override
            public Set<Object> getSingletons() {
                return jaxRsResources;
            }

            @Override
            public Map<String, Object> getProperties() {
                return super.getProperties();
            }
        }));
        return servletContainer;
    }
}
