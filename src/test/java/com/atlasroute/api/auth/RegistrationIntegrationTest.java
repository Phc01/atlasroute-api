package com.atlasroute.api.auth;

import com.atlasroute.api.TestcontainersConfiguration;
import com.atlasroute.api.error.ConflictException;
import com.atlasroute.api.user.Role;
import com.atlasroute.api.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cadastro de ponta a ponta: HTTP → service → BCrypt → PostgreSQL real (Testcontainers).
 * Sem @Transactional no teste: o service não abre transação própria e cada thread precisa
 * ver o que as outras gravaram, então a limpeza é manual no @AfterEach.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    void shouldRegisterAndStoreBcryptHashInDatabase() throws Exception {
        // Arrange
        var body = """
                {"name": "Pedro", "email": " Pedro@AtlasRoute.com ", "password": "senha-forte-123"}
                """;

        // Act
        var result = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // Assert
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("pedro@atlasroute.com"))
                .andExpect(jsonPath("$.role").value("DRIVER"));
        var saved = userRepository.findByEmail("pedro@atlasroute.com").orElseThrow();
        assertThat(saved.getPasswordHash()).startsWith("$2a$10$").isNotEqualTo("senha-forte-123");
        assertThat(saved.getRole()).isEqualTo(Role.DRIVER);
    }

    @Test
    void shouldAcceptOnlyOneOfConcurrentRegistrationsWithSameEmail() throws Exception {
        // Arrange: as threads esperam o mesmo sinal para chamar o service ao mesmo tempo
        int threads = 4;
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(threads);
        List<Future<UserResponse>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            var request = new RegisterRequest("Pedro " + i, "PEDRO@atlasroute.com", "senha-forte-123");
            Callable<UserResponse> task = () -> {
                start.await();
                return registrationService.register(request);
            };
            results.add(executor.submit(task));
        }

        // Act
        start.countDown();
        int succeeded = 0;
        int conflicts = 0;
        for (Future<UserResponse> future : results) {
            try {
                future.get();
                succeeded++;
            } catch (ExecutionException ex) {
                assertThat(ex.getCause()).isInstanceOf(ConflictException.class);
                conflicts++;
            }
        }
        executor.shutdown();

        // Assert: exatamente um cadastro; os outros recebem 409, nunca 500
        assertThat(succeeded).isEqualTo(1);
        assertThat(conflicts).isEqualTo(threads - 1);
        assertThat(userRepository.count()).isEqualTo(1);
    }
}
