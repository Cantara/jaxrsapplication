package no.cantara.security.authentication.test;

import no.cantara.config.ApplicationProperties;
import no.cantara.security.authentication.AuthenticationManagerFactory;

public class FakeAuthenticationManagerFactory implements AuthenticationManagerFactory {

    @Override
    public Class<?> providerClass() {
        return FakeAuthenticationManager.class;
    }

    @Override
    public String alias() {
        return "fake";
    }

    @Override
    public FakeAuthenticationManager create(ApplicationProperties applicationProperties) {
        String defaultFakeUserId = applicationProperties.get("default-fake-user-id", "fake-user");
        String defaultFakeCustomerRef = applicationProperties.get("default-fake-customer-ref", "fake-customer");
        String defaultFakeApplicationId = applicationProperties.get("default-fake-application-id", "fake-application");
        return new FakeAuthenticationManager(defaultFakeUserId, defaultFakeCustomerRef, defaultFakeApplicationId);
    }
}


