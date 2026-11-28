package com.atlasroute.api.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/**
 * Usuário do sistema. Guarda só o hash da senha; quem gera o hash é o service de cadastro.
 *
 * <p>Sem setters: os campos são definidos na criação. O e-mail é normalizado (sem espaços,
 * em minúsculas) para que "Pedro@X.com" e "pedro@x.com" sejam o mesmo usuário.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    // 255 deixa folga para trocar o algoritmo (ex.: Argon2 com prefixo) sem nova migration.
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Exigido pelo JPA. */
    protected User() {
    }

    public User(String name, String email, String passwordHash, Role role, Instant createdAt) {
        this.name = Objects.requireNonNull(name, "name");
        this.email = normalizeEmail(email);
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.role = Objects.requireNonNull(role, "role");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    /** Mesma normalização usada para salvar; use também antes de buscar por e-mail. */
    public static String normalizeEmail(String email) {
        return Objects.requireNonNull(email, "email").trim().toLowerCase(Locale.ROOT);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    // equals/hashCode pelo id, no padrão recomendado para entidades com id gerado:
    // duas instâncias ainda não salvas (id nulo) nunca são iguais.
    // Usa other.getId() (e não other.id) e um hashCode fixo da classe para funcionar também
    // com proxies LAZY do Hibernate, que são subclasses com os campos vazios.
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return User.class.hashCode();
    }
}
