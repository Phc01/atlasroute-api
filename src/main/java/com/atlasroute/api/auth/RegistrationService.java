package com.atlasroute.api.auth;

import com.atlasroute.api.error.ConflictException;
import com.atlasroute.api.user.Role;
import com.atlasroute.api.user.User;
import com.atlasroute.api.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Cadastro de usuários. Todo cadastro público nasce com o perfil {@link Role#DRIVER};
 * promover alguém a ADMIN fica para um fluxo administrativo.
 */
@Service
public class RegistrationService {

    static final String EMAIL_ALREADY_REGISTERED = "E-mail já cadastrado.";
    private static final String EMAIL_UNIQUE_CONSTRAINT = "uk_users_email";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public RegistrationService(UserRepository userRepository, PasswordEncoder passwordEncoder, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = User.normalizeEmail(request.email());

        // Caminho comum: evita gerar o hash (que é lento de propósito) para um e-mail repetido.
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException(EMAIL_ALREADY_REGISTERED);
        }

        User user = new User(request.name(), email, passwordEncoder.encode(request.password()),
                Role.DRIVER, Instant.now(clock));

        try {
            return UserResponse.from(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException ex) {
            // Dois cadastros simultâneos passam pelo existsByEmail; o UNIQUE do banco barra o segundo.
            if (isEmailUniqueViolation(ex)) {
                throw new ConflictException(EMAIL_ALREADY_REGISTERED, ex);
            }
            throw ex;
        }
    }

    private static boolean isEmailUniqueViolation(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause().getMessage();
        return message != null && message.contains(EMAIL_UNIQUE_CONSTRAINT);
    }
}
