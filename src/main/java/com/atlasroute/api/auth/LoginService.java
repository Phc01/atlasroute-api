package com.atlasroute.api.auth;

import com.atlasroute.api.error.UnauthorizedException;
import com.atlasroute.api.user.User;
import com.atlasroute.api.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Login por e-mail e senha.
 *
 * <p>E-mail inexistente e senha errada devolvem a mesma mensagem, e nos dois casos o BCrypt
 * roda (contra um hash fictício quando o usuário não existe), para que nem a resposta nem o
 * tempo de resposta revelem se o e-mail tem cadastro.
 */
@Service
public class LoginService {

    static final String INVALID_CREDENTIALS = "E-mail ou senha inválidos.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Clock clock;
    private final String dummyHash;

    public LoginService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                        JwtService jwtService, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("atlasroute-timing-dummy-password");
    }

    public LoginResponse login(LoginRequest request) {
        Optional<User> user = userRepository.findByEmail(User.normalizeEmail(request.email()));

        String hash = user.map(User::getPasswordHash).orElse(dummyHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);
        if (user.isEmpty() || !passwordMatches) {
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }

        JwtService.AccessToken token = jwtService.issue(user.get());
        long expiresIn = Duration.between(Instant.now(clock), token.expiresAt()).toSeconds();
        return LoginResponse.bearer(token.value(), expiresIn);
    }
}
