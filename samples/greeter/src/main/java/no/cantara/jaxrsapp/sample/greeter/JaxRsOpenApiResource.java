package no.cantara.jaxrsapp.sample.greeter;

import io.swagger.v3.jaxrs2.integration.resources.BaseOpenApiResource;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.ServletConfig;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import no.cantara.jaxrsapp.security.SecurityOverride;

@Path("/openapi.{type:json|yaml}")
public class JaxRsOpenApiResource extends BaseOpenApiResource {
    @Context
    ServletConfig config;
    @Context
    Application app;

    public JaxRsOpenApiResource() {
    }

    @GET
    @Produces({"application/json", "application/yaml"})
    @Operation(
            hidden = true
    )
    @SecurityOverride
    public Response getOpenApi(@Context HttpHeaders headers, @Context UriInfo uriInfo, @PathParam("type") String type) throws Exception {
        return super.getOpenApi(headers, this.config, this.app, uriInfo, type);
    }
}