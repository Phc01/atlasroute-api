package com.atlasroute.api.error;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// controllers = só o de teste: sem isso o slice carrega todos os controllers da aplicação
// (e exigiria mockar as dependências de cada um).
@WebMvcTest(controllers = GlobalExceptionHandlerTest.ErrorTestController.class)
@Import(GlobalExceptionHandlerTest.ErrorTestController.class)
class GlobalExceptionHandlerTest {

    private static final String NOW = "2026-11-27T10:00:00Z";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private Clock clock;

    @BeforeEach
    void setUpClock() {
        when(clock.instant()).thenReturn(Instant.parse(NOW));
    }

    @Test
    void shouldReturn400WithFieldErrorsWhenValidationFails() throws Exception {
        // Arrange
        var body = """
                {"name": "", "weight": -5}
                """;

        // Act
        var result = mockMvc.perform(post("/test/errors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.instance").value("/test/errors"))
                .andExpect(jsonPath("$.timestamp").value(NOW))
                .andExpect(jsonPath("$.errors", hasSize(2)))
                .andExpect(jsonPath("$.errors[?(@.field == 'name')].message").value("é obrigatório"))
                .andExpect(jsonPath("$.errors[?(@.field == 'weight')].message").value("deve ser positivo"));
    }

    @Test
    void shouldReturn400WhenBodyIsMalformed() throws Exception {
        // Arrange
        var body = "{ isso não é json";

        // Act
        var result = mockMvc.perform(post("/test/errors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição inválida"))
                .andExpect(jsonPath("$.detail").value("O corpo da requisição está ausente ou mal formatado."));
    }

    @Test
    void shouldReturn404WhenResourceDoesNotExist() throws Exception {
        // Arrange
        var id = 42;

        // Act
        var result = mockMvc.perform(get("/test/errors/{id}", id));

        // Assert
        result.andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Não encontrado"))
                .andExpect(jsonPath("$.detail").value("Veículo com id 42 não encontrado(a)."))
                .andExpect(jsonPath("$.timestamp").value(NOW));
    }

    @Test
    void shouldReturn404WhenRouteDoesNotExist() throws Exception {
        // Arrange
        var missingRoute = "/api/v1/route-that-does-not-exist";

        // Act
        var result = mockMvc.perform(get(missingRoute));

        // Assert
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Rota não encontrada."))
                .andExpect(jsonPath("$.instance").value(missingRoute));
    }

    @Test
    void shouldReturn500WithoutLeakingInternalMessageWhenErrorIsUnexpected() throws Exception {
        // Arrange
        var route = "/test/errors/unexpected";

        // Act
        var result = mockMvc.perform(get(route));

        // Assert
        result.andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Erro interno"))
                .andExpect(jsonPath("$.detail").value("Ocorreu um erro inesperado. Tente novamente mais tarde."))
                .andExpect(content().string(not(containsString("database password"))));
    }

    /** Controller só de teste, usado para disparar cada tipo de erro. */
    @RestController
    @RequestMapping("/test/errors")
    static class ErrorTestController {

        record TestOrder(@NotBlank(message = "é obrigatório") String name,
                         @Positive(message = "deve ser positivo") double weight) {
        }

        @PostMapping
        void create(@Valid @RequestBody TestOrder order) {
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new IllegalStateException("connection failed: wrong database password");
        }

        @GetMapping("/{id}")
        void findById(@PathVariable Long id) {
            throw new ResourceNotFoundException("Veículo", id);
        }
    }
}
