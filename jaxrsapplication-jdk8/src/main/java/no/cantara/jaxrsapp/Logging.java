package no.cantara.jaxrsapp;

import org.slf4j.bridge.SLF4JBridgeHandler;

public class Logging {

    static {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    public static void init() {
    }
}
