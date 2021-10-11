package no.cantara.jaxrsapp.security;

import no.cantara.security.authentication.Authentication;
import no.cantara.security.authentication.AuthenticationManager;
import no.cantara.security.authentication.AuthenticationResult;
import no.cantara.security.authorization.AccessManager;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
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
        String authorizationHeader = requestContext.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        AuthenticationResult authenticationResult = authenticationManager.authenticate(authorizationHeader);
        if (!authenticationResult.isValid()) {
            // authentication failed
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
            return;
        }
        final Authentication authentication = authenticationResult.authentication();
        requestContext.setProperty(Authentication.class.getName(), authentication);
        Method resourceMethod = jaxRsEndpointResolver.apply(requestContext);
        SecureAction secureAction = resourceMethod.getDeclaredAnnotation(SecureAction.class);
        if (secureAction == null) {
            // forbid access to endpoint without secure-action annotation, i.e. secure-by-default
            requestContext.abortWith(Response.status(Response.Status.FORBIDDEN).build());
            return;
        }
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
