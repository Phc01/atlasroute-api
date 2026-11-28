package com.atlasroute.api.auth;

import com.atlasroute.api.error.ConflictException;
import com.atlasroute.api.user.Role;
import com.atlasroute.api.user.User;
import com.atlasroute.api.user.UserRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

/**
 * Cadastro de usuários. Todo cadastro público nasce com o perfil {@link Role#DRIVER};
 * promover alguém a ADMIN fica para um fluxo administrativo.
 *
 * <p>Sem {@code @Transactional} de propósito: o hash do BCrypt é lento (~100 ms) e não deve
 * segurar uma conexão do banco. {@code existsByEmail} e {@code saveAndFlush} já rodam cada um
 * na sua transação, e o UNIQUE do banco garante a consistência.
 *
 * <p>Decisão consciente: o 409 revela que o e-mail já tem cadastro (troca comum em cadastro
 * público). Limite de requisições entra junto com a segurança (tarefa 5).
 */
@Service
public class RegistrationService {

    static final String EMAIL_ALREADY_REGISTERED = "E-mail já cadastrado.";
    static final String EMAIL_UNIQUE_CONSTRAINT = "uk_users_email";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public RegistrationService(UserRepository userRepository, PasswordEncoder passwordEncoder, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

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

    /** Usa o nome da constraint que o Hibernate extrai do erro, sem depender do texto da mensagem. */
    private static boolean isEmailUniqueViolation(DataIntegrityViolationException ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                return EMAIL_UNIQUE_CONSTRAINT.equalsIgnoreCase(violation.getConstraintName());
            }
        }
        return false;
    }
}
