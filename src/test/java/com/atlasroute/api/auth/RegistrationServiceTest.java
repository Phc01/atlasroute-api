package com.atlasroute.api.auth;

import com.atlasroute.api.error.ConflictException;
import com.atlasroute.api.user.Role;
import com.atlasroute.api.user.User;
import com.atlasroute.api.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-28T15:00:00Z");

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        registrationService = new RegistrationService(userRepository, passwordEncoder, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldRegisterDriverWithHashedPasswordAndNormalizedEmail() {
        // Arrange
        var request = new RegisterRequest("Pedro", "Pedro@AtlasRoute.com", "senha-forte-123");
        when(userRepository.existsByEmail("pedro@atlasroute.com")).thenReturn(false);
        when(passwordEncoder.encode("senha-forte-123")).thenReturn("hash-bcrypt");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var response = registrationService.register(request);

        // Assert
        var captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("pedro@atlasroute.com");
        assertThat(saved.getPasswordHash()).isEqualTo("hash-bcrypt");
        assertThat(saved.getRole()).isEqualTo(Role.DRIVER);
        assertThat(saved.getCreatedAt()).isEqualTo(NOW);
        assertThat(response.email()).isEqualTo("pedro@atlasroute.com");
        assertThat(response.role()).isEqualTo(Role.DRIVER);
    }

    @Test
    void shouldThrowConflictWithoutHashingWhenEmailAlreadyExists() {
        // Arrange
        var request = new RegisterRequest("Pedro", "pedro@atlasroute.com", "senha-forte-123");
        when(userRepository.existsByEmail("pedro@atlasroute.com")).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> registrationService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("E-mail já cadastrado.");
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldThrowConflictWhenConcurrentRegistrationHitsUniqueConstraint() {
        // Arrange
        var request = new RegisterRequest("Pedro", "pedro@atlasroute.com", "senha-forte-123");
        when(userRepository.existsByEmail("pedro@atlasroute.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        var cause = new ConstraintViolationException("duplicate key", new SQLException("duplicate key"), "uk_users_email");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("could not execute statement", cause));

        // Act + Assert
        assertThatThrownBy(() -> registrationService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("E-mail já cadastrado.");
    }

    @Test
    void shouldRethrowOtherIntegrityViolations() {
        // Arrange
        var request = new RegisterRequest("Pedro", "pedro@atlasroute.com", "senha-forte-123");
        when(userRepository.existsByEmail("pedro@atlasroute.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        var cause = new ConstraintViolationException("check violation", new SQLException("check"), "ck_users_role");
        var violation = new DataIntegrityViolationException("could not execute statement", cause);
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(violation);

        // Act + Assert
        assertThatThrownBy(() -> registrationService.register(request)).isSameAs(violation);
    }
}
