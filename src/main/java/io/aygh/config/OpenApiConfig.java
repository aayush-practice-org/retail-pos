package io.aygh.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger, with the bearer scheme applied to every operation by default.
 * <p>
 * Applying it globally and exempting the few anonymous endpoints with
 * {@code @SecurityRequirements} is the safer default: a new endpoint is
 * documented as protected unless someone deliberately says otherwise.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI martOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Mart Service API")
                        .version("v1")
                        .description("""
                                Multi-tenant mart back office. Every mart's data lives in its own
                                PostgreSQL schema; accounts are shared in `public` and carry the slug
                                of the schema their work is routed to.

                                Sign in at `/public/auth/login` and send the token as
                                `Authorization: Bearer <token>`.
                                """))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("PASETO")
                                .description("A PASETO v4.local token issued by /public/auth/login")));
    }
}
