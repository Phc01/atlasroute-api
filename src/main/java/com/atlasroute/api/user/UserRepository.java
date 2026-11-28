package com.atlasroute.api.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Acesso à tabela {@code users}.
 *
 * <p>As buscas por e-mail normalizam o valor recebido (espaços nas pontas, maiúsculas) na própria
 * consulta, do mesmo jeito que ele é salvo. Assim o service pode passar o e-mail como o usuário
 * digitou, e não existe uma versão "crua" que falharia com "Pedro@X.com".
 */
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("select u from User u where u.email = lower(trim(:email))")
    Optional<User> findByEmail(@Param("email") String email);

    @Query("select count(u) > 0 from User u where u.email = lower(trim(:email))")
    boolean existsByEmail(@Param("email") String email);
}
