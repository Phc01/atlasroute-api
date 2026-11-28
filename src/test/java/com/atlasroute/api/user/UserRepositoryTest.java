package com.atlasroute.api.user;

import com.atlasroute.api.TestcontainersConfiguration;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Roda contra o PostgreSQL real com o schema do Flyway, então também valida a migration V2
 * (tipos, UNIQUE e CHECKs) e o mapeamento da entidade com ddl-auto: validate.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class UserRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-11-28T12:00:00Z");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldSaveUserAndFindByEmail() {
        // Arrange
        var user = new User("Pedro", "pedro@atlasroute.com", "hash-bcrypt", Role.ADMIN, NOW);

        // Act
        userRepository.saveAndFlush(user);
        var found = userRepository.findByEmail("pedro@atlasroute.com");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isNotNull();
        assertThat(found.get().getName()).isEqualTo("Pedro");
        assertThat(found.get().getPasswordHash()).isEqualTo("hash-bcrypt");
        assertThat(found.get().getRole()).isEqualTo(Role.ADMIN);
        assertThat(found.get().getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldNormalizeEmailToLowercaseWithoutSpaces() {
        // Arrange
        var user = new User("Ana", "  Ana.Driver@AtlasRoute.COM ", "hash", Role.DRIVER, NOW);

        // Act
        userRepository.saveAndFlush(user);

        // Assert
        assertThat(user.getEmail()).isEqualTo("ana.driver@atlasroute.com");
    }

    @Test
    void shouldFindByEmailIgnoringFormatWhileExactSearchDoesNot() {
        // Arrange
        userRepository.saveAndFlush(new User("Pedro", "pedro@atlasroute.com", "hash", Role.ADMIN, NOW));
        var typedEmail = " PEDRO@AtlasRoute.com ";

        // Act
        var exact = userRepository.findByEmail(typedEmail);
        var ignoringFormat = userRepository.findByEmailIgnoringFormat(typedEmail);

        // Assert
        assertThat(exact).isEmpty();
        assertThat(ignoringFormat).isPresent();
        assertThat(userRepository.existsByEmailIgnoringFormat(typedEmail)).isTrue();
    }

    @Test
    void shouldReturnFalseWhenEmailIsNotRegistered() {
        // Arrange
        var email = "ninguem@atlasroute.com";

        // Act
        var exists = userRepository.existsByEmailIgnoringFormat(email);

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    void shouldRejectDuplicateEmail() {
        // Arrange
        userRepository.saveAndFlush(new User("Pedro", "pedro@atlasroute.com", "hash", Role.ADMIN, NOW));
        var duplicate = new User("Outro Pedro", "PEDRO@atlasroute.com", "hash", Role.DRIVER, NOW);

        // Act + Assert
        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectNonNormalizedEmailInsertedOutsideTheApplication() {
        // Arrange
        var sql = "INSERT INTO users (name, email, password_hash, role, created_at) "
                + "VALUES ('Seed', 'Seed@AtlasRoute.com', 'hash', 'ADMIN', now())";

        // Act + Assert
        assertThatThrownBy(() -> jdbcTemplate.update(sql))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_users_email_normalized");
    }

    @Test
    void shouldConsiderUserEqualToItsLazyProxy() {
        // Arrange
        var id = entityManager.persistFlushFind(
                new User("Pedro", "pedro@atlasroute.com", "hash", Role.ADMIN, NOW)).getId();
        entityManager.clear();
        var loaded = entityManager.find(User.class, id);
        entityManager.clear();
        var proxy = entityManager.getEntityManager().getReference(User.class, id);
        // Confere antes do Act: chamar equals no proxy o inicializa.
        assertThat(Hibernate.isInitialized(proxy)).as("deve ser um proxy LAZY").isFalse();

        // Act
        var loadedEqualsProxy = loaded.equals(proxy);
        var proxyEqualsLoaded = proxy.equals(loaded);

        // Assert
        assertThat(loadedEqualsProxy).isTrue();
        assertThat(proxyEqualsLoaded).isTrue();
        assertThat(loaded.hashCode()).isEqualTo(proxy.hashCode());
    }

    @Test
    void shouldNotConsiderUnsavedUsersEqual() {
        // Arrange
        var first = new User("Pedro", "pedro@atlasroute.com", "hash", Role.ADMIN, NOW);
        var second = new User("Pedro", "pedro@atlasroute.com", "hash", Role.ADMIN, NOW);

        // Act
        var equal = first.equals(second);

        // Assert
        assertThat(equal).isFalse();
    }
}
