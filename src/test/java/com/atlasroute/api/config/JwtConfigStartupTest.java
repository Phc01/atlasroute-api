package com.atlasroute.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A aplicação não pode subir com um segredo de JWT ausente ou fraco: um token forjado com um
 * segredo conhecido daria acesso de ADMIN. Sobe só o {@link JwtConfig}, sem o resto da aplicação.
 */
class JwtConfigStartupTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(JwtConfig.class, ClockConfig.class)
            .withPropertyValues(
                    "atlasroute.security.jwt.expiration=1h",
                    "atlasroute.security.jwt.issuer=atlasroute-api");

    @Test
    void shouldFailToStartWithoutSecret() {
        // Arrange
        var withoutSecret = runner.withPropertyValues("atlasroute.security.jwt.secret=");

        // Act + Assert
        withoutSecret.run(context -> assertThat(context)
                .hasFailed()
                .getFailure().rootCause().hasMessageContaining("JWT_SECRET"));
    }

    @Test
    void shouldFailToStartWithSecretShorterThan32Bytes() {
        // Arrange
        var weakSecret = runner.withPropertyValues("atlasroute.security.jwt.secret=curto-demais");

        // Act + Assert
        weakSecret.run(context -> assertThat(context)
                .hasFailed()
                .getFailure().rootCause().hasMessageContaining("32 bytes"));
    }

    @Test
    void shouldStartWithValidSecret() {
        // Arrange
        var validSecret = runner.withPropertyValues(
                "atlasroute.security.jwt.secret=um-segredo-valido-com-mais-de-32-bytes-aqui");

        // Act + Assert
        validSecret.run(context -> assertThat(context).hasNotFailed().hasSingleBean(JwtDecoder.class));
    }
}
