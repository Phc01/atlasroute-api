package com.atlasroute.api.auth;

import com.atlasroute.api.user.UserResponse;
import com.atlasroute.api.error.ConflictException;
import com.atlasroute.api.error.UnauthorizedException;
import com.atlasroute.api.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Filtros de segurança desligados: aqui se testa o controller; as regras de acesso ficam no SecurityConfigTest.
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    private static final String REGISTER = "/api/v1/auth/register";
    private static final String LOGIN = "/api/v1/auth/login";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistrationService registrationService;

    @MockitoBean
    private LoginService loginService;

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
                {"name": "  Pedro ", "email": " Pedro@AtlasRoute.com ", "password": " senha-forte-123 "}
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
        var captor = ArgumentCaptor.forClass(RegisterRequest.class);
        verify(registrationService).register(captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("Pedro");
        assertThat(captor.getValue().email()).isEqualTo("Pedro@AtlasRoute.com");
        assertThat(captor.getValue().password()).as("a senha não é alterada").isEqualTo(" senha-forte-123 ");
    }

    @Test
    void shouldReturn400WhenPasswordExceeds72BytesEvenWithFewerCharacters() throws Exception {
        // Arrange: 40 caracteres "ç" = 80 bytes em UTF-8
        var password = "ç".repeat(40);
        var body = """
                {"name": "Pedro", "email": "pedro@atlasroute.com", "password": "%s"}
                """.formatted(password);

        // Act
        var result = mockMvc.perform(post(REGISTER).contentType(MediaType.APPLICATION_JSON).content(body));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].field").value("password"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("deve ter no máximo 72 bytes (letras com acento e emojis contam mais de 1)"));
        verifyNoInteractions(registrationService);
    }

    @Test
    void shouldReturn400WhenBodyIsMissing() throws Exception {
        // Arrange
        var request = post(REGISTER).contentType(MediaType.APPLICATION_JSON);

        // Act
        var result = mockMvc.perform(request);

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição inválida"));
        verifyNoInteractions(registrationService);
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
                        .value("deve ter no mínimo 8 caracteres"));
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

    @Test
    void shouldReturn200WithBearerTokenWhenLoginSucceeds() throws Exception {
        // Arrange
        var body = """
                {"email": " pedro@atlasroute.com ", "password": "senha-forte-123"}
                """;
        when(loginService.login(any())).thenReturn(LoginResponse.bearer("jwt.token.aqui", 3600));

        // Act
        var result = mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(body));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt.token.aqui"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
        verify(loginService).login(new LoginRequest("pedro@atlasroute.com", "senha-forte-123"));
    }

    @Test
    void shouldReturn401WhenCredentialsAreInvalid() throws Exception {
        // Arrange
        var body = """
                {"email": "pedro@atlasroute.com", "password": "senha-errada"}
                """;
        when(loginService.login(any())).thenThrow(new UnauthorizedException("E-mail ou senha inválidos."));

        // Act
        var result = mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(body));

        // Assert
        result.andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Não autorizado"))
                .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos."));
    }

    @Test
    void shouldReturn400WhenLoginPasswordExceeds72Bytes() throws Exception {
        // Arrange
        var body = """
                {"email": "pedro@atlasroute.com", "password": "%s"}
                """.formatted("a".repeat(10_000));

        // Act
        var result = mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(body));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));
        verifyNoInteractions(loginService);
    }

    @Test
    void shouldReturn400WhenLoginFieldsAreMissing() throws Exception {
        // Arrange
        var body = """
                {"email": "", "password": ""}
                """;

        // Act
        var result = mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(body));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(2)));
        verifyNoInteractions(loginService);
    }
}
