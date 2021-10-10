package no.cantara.jaxrsapp.test.greet;

import no.cantara.config.ApplicationProperties;
import no.cantara.jaxrsapp.AbstractJaxRsServletApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GreetApplication extends AbstractJaxRsServletApplication<GreetApplication> {

    private static final Logger log = LoggerFactory.getLogger(GreetApplication.class);

    public static void main(String[] args) {
        GreetApplication application = new GreetApplication(ApplicationProperties.builder().defaults().build());
        application.init();
        application.start();
    }

    public GreetApplication(ApplicationProperties config) {
        super(config);
    }

    @Override
    public GreetApplication init() {
        initAndRegisterJaxRsWsComponent(GreetingResource.class, this::initGreetingResource);
        return this;
    }

    private GreetingResource initGreetingResource() {
        return new GreetingResource();
    }
}
