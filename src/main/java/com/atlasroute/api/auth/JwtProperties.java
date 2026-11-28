package com.atlasroute.api.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Configuração do JWT ({@code atlasroute.security.jwt.*} no application.yml).
 *
 * @param secret     chave HMAC (HS256); precisa ter pelo menos 32 bytes
 * @param expiration validade do token de acesso (ex.: 1h, 30m)
 * @param issuer     valor do claim {@code iss}, conferido na validação
 */
@Validated
@ConfigurationProperties("atlasroute.security.jwt")
public record JwtProperties(
        @NotBlank String secret,
        @NotNull Duration expiration,
        @NotBlank String issuer
) {
}
