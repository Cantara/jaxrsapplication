package no.cantara.jaxrsapp.security;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import no.cantara.security.authentication.Authentication;
import no.cantara.security.authentication.AuthenticationManager;
import no.cantara.security.authentication.AuthenticationResult;
import no.cantara.security.authorization.AccessManager;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Function;

public class SecurityFilter implements ContainerRequestFilter {

    private final AuthenticationManager authenticationManager;
    private final AccessManager accessManager;
    private final Function<ContainerRequestContext, Method> jaxRsEndpointResolver;

    public SecurityFilter(AuthenticationManager authenticationManager, AccessManager accessManager, Function<ContainerRequestContext, Method> jaxRsEndpointResolver) {
        Objects.requireNonNull(authenticationManager);
        Objects.requireNonNull(accessManager);
        Objects.requireNonNull(jaxRsEndpointResolver);
        this.authenticationManager = authenticationManager;
        this.accessManager = accessManager;
        this.jaxRsEndpointResolver = jaxRsEndpointResolver;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        Method resourceMethod = jaxRsEndpointResolver.apply(requestContext);
        SecurityOverride securityOverride = resourceMethod.getDeclaredAnnotation(SecurityOverride.class);
        if (securityOverride != null) {
            return; // access granted, no authentication or access-check needed
        }
        SecureAction secureAction = resourceMethod.getDeclaredAnnotation(SecureAction.class);
        if (secureAction == null) {
            // forbid access to endpoint without secure-action annotation, i.e. secure-by-default
            requestContext.abortWith(Response.status(Response.Status.FORBIDDEN).build());
            return;
        }
        String authorizationHeader = requestContext.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        AuthenticationResult authenticationResult = authenticationManager.authenticate(authorizationHeader);
        if (!authenticationResult.isValid()) {
            // authentication failed
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
            return;
        }
        final Authentication authentication = authenticationResult.authentication();
        requestContext.setProperty(Authentication.class.getName(), authentication);
        String action = secureAction.value();
        boolean hasAccess = accessManager.hasAccess(authentication, action);
        if (!hasAccess) {
            // access not allowed according to rules
            requestContext.abortWith(Response.status(Response.Status.FORBIDDEN).build());
            return;
        }
        // access granted
        requestContext.setSecurityContext(new JaxRsAppSecurityContext(authentication));
    }
}
