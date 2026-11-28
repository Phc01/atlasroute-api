package com.atlasroute.api.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Acesso à tabela {@code users}.
 *
 * <p>Nos services, use os métodos {@code ...IgnoringFormat}: eles normalizam o e-mail digitado
 * (espaços, maiúsculas) antes de buscar, do mesmo jeito que ele foi salvo. Os métodos derivados
 * {@code findByEmail}/{@code existsByEmail} comparam o texto exato.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    default Optional<User> findByEmailIgnoringFormat(String rawEmail) {
        return findByEmail(User.normalizeEmail(rawEmail));
    }

    default boolean existsByEmailIgnoringFormat(String rawEmail) {
        return existsByEmail(User.normalizeEmail(rawEmail));
    }
}
