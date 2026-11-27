package com.atlasroute.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Sobe a aplicação inteira contra um PostgreSQL real (Testcontainers) e
 * garante que o contexto carrega e as migrations do Flyway rodam.
 * Precisa do Docker rodando.
 */
@SpringBootTest
@Testcontainers
class AtlasRouteApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Test
    void shouldLoadContextAndRunMigrations() {
        // Se o Flyway ou a conexão com o banco falharem, o contexto não sobe e o teste quebra.
    }
}
