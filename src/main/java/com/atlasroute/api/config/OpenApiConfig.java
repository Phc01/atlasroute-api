package com.atlasroute.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger com o botão "Authorize": cole o {@code accessToken} do login (sem "Bearer ")
 * e as chamadas seguintes vão com {@code Authorization: Bearer <token>}.
 */
@Configuration
public class OpenApiConfig {

    static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI atlasRouteOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("AtlasRoute API")
                        .description("API de logística com otimização de rotas e distribuição de cargas")
                        .version("v1"))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token do POST /api/v1/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
