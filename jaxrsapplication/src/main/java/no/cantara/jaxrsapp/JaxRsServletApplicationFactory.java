package no.cantara.jaxrsapp;

import no.cantara.config.ProviderFactory;

public interface JaxRsServletApplicationFactory<A extends JaxRsServletApplication<A>> extends ProviderFactory<JaxRsServletApplication<A>> {
}
