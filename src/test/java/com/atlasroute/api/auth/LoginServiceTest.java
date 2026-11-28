package com.atlasroute.api.auth;

import com.atlasroute.api.error.UnauthorizedException;
import com.atlasroute.api.user.Role;
import com.atlasroute.api.user.User;
import com.atlasroute.api.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-28T18:00:00Z");
    private static final String DUMMY_HASH = "hash-ficticio";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private LoginService loginService;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode(any())).thenReturn(DUMMY_HASH);
        loginService = new LoginService(userRepository, passwordEncoder, jwtService, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldReturnBearerTokenWhenCredentialsAreValid() {
        // Arrange
        var user = new User("Pedro", "pedro@atlasroute.com", "hash-real", Role.DRIVER, NOW);
        when(userRepository.findByEmail("pedro@atlasroute.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senha-forte-123", "hash-real")).thenReturn(true);
        when(jwtService.issue(user)).thenReturn(new JwtService.AccessToken("jwt.token.aqui", NOW.plus(Duration.ofHours(1))));

        // Act
        var response = loginService.login(new LoginRequest(" Pedro@AtlasRoute.com ", "senha-forte-123"));

        // Assert
        assertThat(response.accessToken()).isEqualTo("jwt.token.aqui");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600);
    }

    @Test
    void shouldReturnGenericErrorWhenPasswordIsWrong() {
        // Arrange
        var user = new User("Pedro", "pedro@atlasroute.com", "hash-real", Role.DRIVER, NOW);
        when(userRepository.findByEmail("pedro@atlasroute.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senha-errada", "hash-real")).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> loginService.login(new LoginRequest("pedro@atlasroute.com", "senha-errada")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("E-mail ou senha inválidos.");
        verify(jwtService, never()).issue(any());
    }

    @Test
    void shouldReturnSameGenericErrorAndStillRunBcryptWhenEmailDoesNotExist() {
        // Arrange
        when(userRepository.findByEmail("ninguem@atlasroute.com")).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> loginService.login(new LoginRequest("ninguem@atlasroute.com", "qualquer-senha")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("E-mail ou senha inválidos.");
        // O BCrypt roda contra o hash fictício: o tempo de resposta não revela se o e-mail existe.
        verify(passwordEncoder).matches("qualquer-senha", DUMMY_HASH);
        verify(jwtService, never()).issue(any());
    }
}
