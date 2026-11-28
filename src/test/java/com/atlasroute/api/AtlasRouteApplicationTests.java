package com.atlasroute.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Sobe a aplicação inteira contra um PostgreSQL real (Testcontainers) e
 * garante que o contexto carrega e as migrations do Flyway rodam.
 * Precisa do Docker rodando.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AtlasRouteApplicationTests {

    @Test
    void shouldLoadContextAndRunMigrations() {
        // Se o Flyway ou a conexão com o banco falharem, o contexto não sobe e o teste quebra.
    }
}
