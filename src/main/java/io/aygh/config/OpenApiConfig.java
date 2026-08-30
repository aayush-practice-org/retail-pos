package io.aygh.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
 *  OpenAPI / Swagger UI.
 *
 *  Served at /swagger-ui.html, with the raw document at /v3/api-docs. Both sit in
 *  their own permitAll chain in SecurityConfig, so the docs are reachable without
 *  a token even though almost nothing they describe is.
 *
 *  The document is split into the same three groups the security chains are:
 *  auth (the way in), admin (the back office, token required) and api (the rest of
 *  the signed-in surface). "Authorize" takes the PASETO token that POST
 *  /api/auth/login hands back — paste it raw, Swagger adds the "Bearer " prefix
 *  PasetoAuthenticationFilter is looking for.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI martOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Mart Service API")
                        .description("""
                                Back office and storefront API for the mart service.

                                Staff endpoints expect the PASETO token returned by \
                                `POST /api/auth/login`, sent as `Authorization: Bearer <token>`.""")
                        .version("v1")
                        .contact(new Contact().name("Mart Service")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("PASETO")
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")
                                .description("PASETO token from POST /api/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("1-auth")
                .displayName("Auth")
                .pathsToMatch("/api/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("2-admin")
                .displayName("Admin")
                .pathsToMatch("/admin/**")
                .build();
    }

    @Bean
    public GroupedOpenApi api() {
        return GroupedOpenApi.builder()
                .group("3-api")
                .displayName("API")
                .pathsToMatch("/api/**", "/health-check")
                .pathsToExclude("/api/auth/**")
                .build();
    }
}
