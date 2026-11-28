package com.atlasroute.api.auth;

import com.atlasroute.api.TestcontainersConfiguration;
import com.atlasroute.api.user.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Login de ponta a ponta: usuário cadastrado de verdade (BCrypt real) no PostgreSQL. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class LoginIntegrationTest {

    private static final String LOGIN = "/api/v1/auth/login";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    private Long userId;

    @BeforeEach
    void registerUser() {
        userId = registrationService.register(
                new RegisterRequest("Pedro", "pedro@atlasroute.com", "senha-forte-123")).id();
    }

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    void shouldLoginAndReturnValidTokenForTheUser() throws Exception {
        // Arrange
        var body = """
                {"email": "PEDRO@atlasroute.com", "password": "senha-forte-123"}
                """;

        // Act
        var result = mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(body));

        // Assert
        var json = result.andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andReturn().getResponse().getContentAsString();
        var jwt = jwtService.validate(JsonPath.read(json, "$.accessToken"));
        assertThat(jwt.getSubject()).isEqualTo(String.valueOf(userId));
        assertThat(jwt.getClaimAsString(JwtService.CLAIM_ROLE)).isEqualTo("DRIVER");
        assertThat(jwt.getClaimAsString(JwtService.CLAIM_EMAIL)).isEqualTo("pedro@atlasroute.com");
    }

    @Test
    void shouldReturnSame401ForWrongPasswordAndUnknownEmail() throws Exception {
        // Arrange
        var wrongPassword = """
                {"email": "pedro@atlasroute.com", "password": "senha-errada"}
                """;
        var unknownEmail = """
                {"email": "ninguem@atlasroute.com", "password": "senha-forte-123"}
                """;

        // Act
        var first = mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(wrongPassword));
        var second = mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(unknownEmail));

        // Assert
        first.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos."));
        second.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos."));
    }
}
