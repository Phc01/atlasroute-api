package com.atlasroute.api.auth;

import com.atlasroute.api.config.JwtConfig;
import com.atlasroute.api.user.Role;
import com.atlasroute.api.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Usa o encoder/decoder reais do {@link JwtConfig}, sem subir o Spring. */
class JwtServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-28T18:00:00Z");
    private static final String SECRET = "segredo-de-teste-com-pelo-menos-32-bytes!";
    private static final JwtProperties PROPERTIES =
            new JwtProperties(SECRET, Duration.ofHours(1), "atlasroute-api");

    private final JwtConfig config = new JwtConfig();

    @Test
    void shouldIssueTokenWithUserClaimsAndExpiration() {
        // Arrange
        var service = serviceAt(NOW, PROPERTIES);

        // Act
        var token = service.issue(user(42L, Role.ADMIN));
        var jwt = service.validate(token.value());

        // Assert
        assertThat(token.expiresAt()).isEqualTo(NOW.plus(Duration.ofHours(1)));
        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("atlasroute-api");
        assertThat(jwt.getClaimAsString(JwtService.CLAIM_EMAIL)).isEqualTo("pedro@atlasroute.com");
        assertThat(jwt.getClaimAsString(JwtService.CLAIM_ROLE)).isEqualTo("ADMIN");
        assertThat(jwt.getIssuedAt()).isEqualTo(NOW);
        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
    }

    @Test
    void shouldRejectExpiredToken() {
        // Arrange: token emitido agora, validado 1h01min depois (tolerância de 30s)
        var token = serviceAt(NOW, PROPERTIES).issue(user(1L, Role.DRIVER)).value();
        var decoderLater = decoderAt(NOW.plus(Duration.ofMinutes(61)), PROPERTIES);

        // Act + Assert
        assertThatThrownBy(() -> decoderLater.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectTamperedToken() {
        // Arrange: troca um caractere do payload (parte do meio do token)
        var service = serviceAt(NOW, PROPERTIES);
        var parts = service.issue(user(1L, Role.DRIVER)).value().split("\\.");
        char changed = parts[1].charAt(10) == 'A' ? 'B' : 'A';
        var tampered = parts[0] + "." + parts[1].substring(0, 10) + changed + parts[1].substring(11) + "." + parts[2];

        // Act + Assert
        assertThatThrownBy(() -> service.validate(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectTokenSignedWithAnotherSecret() {
        // Arrange
        var otherSecret = new JwtProperties("outro-segredo-qualquer-com-32-bytes-ou-mais", Duration.ofHours(1),
                "atlasroute-api");
        var token = serviceAt(NOW, otherSecret).issue(user(1L, Role.DRIVER)).value();

        // Act + Assert
        assertThatThrownBy(() -> serviceAt(NOW, PROPERTIES).validate(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectTokenFromAnotherIssuer() {
        // Arrange
        var otherIssuer = new JwtProperties(SECRET, Duration.ofHours(1), "outro-sistema");
        var token = serviceAt(NOW, otherIssuer).issue(user(1L, Role.DRIVER)).value();

        // Act + Assert
        assertThatThrownBy(() -> serviceAt(NOW, PROPERTIES).validate(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRefuseSecretShorterThan32Bytes() {
        // Arrange
        var weak = new JwtProperties("curto-demais", Duration.ofHours(1), "atlasroute-api");

        // Act + Assert
        assertThatThrownBy(() -> config.jwtSecretKey(weak))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    private JwtService serviceAt(Instant instant, JwtProperties properties) {
        var clock = Clock.fixed(instant, ZoneOffset.UTC);
        var key = config.jwtSecretKey(properties);
        return new JwtService(config.jwtEncoder(key), config.jwtDecoder(key, properties, clock), properties, clock);
    }

    private JwtDecoder decoderAt(Instant instant, JwtProperties properties) {
        return config.jwtDecoder(config.jwtSecretKey(properties), properties, Clock.fixed(instant, ZoneOffset.UTC));
    }

    private static User user(Long id, Role role) {
        var user = new User("Pedro", "pedro@atlasroute.com", "hash", role, NOW);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
