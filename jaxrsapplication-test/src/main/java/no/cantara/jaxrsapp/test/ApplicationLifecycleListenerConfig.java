package no.cantara.jaxrsapp.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApplicationLifecycleListenerConfig {
    /**
     * A {@link JaxRsServletApplicationLifecycleListener} class to instantiate and use as lifecycle-listener.
     */
    Class<? extends JaxRsServletApplicationLifecycleListener>[] value();

    String application() default "";
}