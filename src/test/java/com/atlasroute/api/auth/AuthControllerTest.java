package com.atlasroute.api.auth;

import com.atlasroute.api.error.ConflictException;
import com.atlasroute.api.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    private static final String REGISTER = "/api/v1/auth/register";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistrationService registrationService;

    @MockitoBean
    private Clock clock;

    @BeforeEach
    void setUpClock() {
        when(clock.instant()).thenReturn(Instant.parse("2026-11-28T15:00:00Z"));
    }

    @Test
    void shouldReturn201WithUserDataWithoutPassword() throws Exception {
        // Arrange
        var body = """
                {"name": "Pedro", "email": " Pedro@AtlasRoute.com ", "password": "senha-forte-123"}
                """;
        when(registrationService.register(any()))
                .thenReturn(new UserResponse(1L, "Pedro", "pedro@atlasroute.com", Role.DRIVER));

        // Act
        var result = mockMvc.perform(post(REGISTER).contentType(MediaType.APPLICATION_JSON).content(body));

        // Assert
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("pedro@atlasroute.com"))
                .andExpect(jsonPath("$.role").value("DRIVER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(content().string(not(containsString("senha-forte-123"))));
        verify(registrationService).register(new RegisterRequest("Pedro", "Pedro@AtlasRoute.com", "senha-forte-123"));
    }

    @Test
    void shouldReturn400WithEachInvalidField() throws Exception {
        // Arrange
        var body = """
                {"name": "", "email": "nao-e-email", "password": "curta"}
                """;

        // Act
        var result = mockMvc.perform(post(REGISTER).contentType(MediaType.APPLICATION_JSON).content(body));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.errors", hasSize(3)))
                .andExpect(jsonPath("$.errors[?(@.field == 'name')].message").value("é obrigatório"))
                .andExpect(jsonPath("$.errors[?(@.field == 'email')].message").value("deve ser um e-mail válido"))
                .andExpect(jsonPath("$.errors[?(@.field == 'password')].message")
                        .value("deve ter entre 8 e 72 caracteres"));
    }

    @Test
    void shouldReturn409WhenEmailIsAlreadyRegistered() throws Exception {
        // Arrange
        var body = """
                {"name": "Pedro", "email": "pedro@atlasroute.com", "password": "senha-forte-123"}
                """;
        when(registrationService.register(any())).thenThrow(new ConflictException("E-mail já cadastrado."));

        // Act
        var result = mockMvc.perform(post(REGISTER).contentType(MediaType.APPLICATION_JSON).content(body));

        // Assert
        result.andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Conflito"))
                .andExpect(jsonPath("$.detail").value("E-mail já cadastrado."));
    }
}
