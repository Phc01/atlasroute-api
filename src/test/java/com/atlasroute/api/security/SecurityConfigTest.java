package com.atlasroute.api.security;

import com.atlasroute.api.auth.AuthController;
import com.atlasroute.api.auth.JwtProperties;
import com.atlasroute.api.auth.JwtService;
import com.atlasroute.api.auth.LoginResponse;
import com.atlasroute.api.auth.LoginService;
import com.atlasroute.api.auth.RegistrationService;
import com.atlasroute.api.config.ClockConfig;
import com.atlasroute.api.config.JwtConfig;
import com.atlasroute.api.status.StatusController;
import com.atlasroute.api.user.Role;
import com.atlasroute.api.user.User;
import com.atlasroute.api.user.UserController;
import com.atlasroute.api.user.UserResponse;
import com.atlasroute.api.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regras de acesso com o filtro de segurança real e tokens de verdade (assinados pelo
 * {@link JwtService}). Os services são mocks: aqui só importa quem passa e quem é barrado.
 */
@WebMvcTest(controllers = {UserController.class, StatusController.class, AuthController.class})
@Import({SecurityConfig.class, ProblemDetailSecurityHandler.class, JwtConfig.class, ClockConfig.class, JwtService.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private JwtProperties jwtProperties;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private RegistrationService registrationService;

    @MockitoBean
    private LoginService loginService;

    @Test
    void shouldAllowPublicRoutesWithoutToken() throws Exception {
        // Arrange
        when(loginService.login(any())).thenReturn(LoginResponse.bearer("jwt", 3600));
        var loginBody = """
                {"email": "pedro@atlasroute.com", "password": "senha-forte-123"}
                """;

        // Act
        var status = mockMvc.perform(get("/api/v1/status"));
        var login = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody));

        // Assert: POST público funciona sem token de CSRF (API stateless)
        status.andExpect(status().isOk());
        login.andExpect(status().isOk());
    }

    @Test
    void shouldReturn401ProblemDetailWithoutToken() throws Exception {
        // Arrange
        var route = "/api/v1/users/me";

        // Act
        var result = mockMvc.perform(get(route));

        // Assert
        result.andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Não autorizado"))
                .andExpect(jsonPath("$.instance").value(route))
                .andExpect(jsonPath("$.timestamp").exists());
        verifyNoInteractions(userService);
    }

    @Test
    void shouldReturn401WhenTokenIsInvalid() throws Exception {
        // Arrange
        var garbage = "Bearer isso.nao.e-um-jwt";

        // Act
        var result = mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, garbage));

        // Assert
        result.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Não autorizado"));
    }

    @Test
    void shouldReturn401WhenTokenIsExpired() throws Exception {
        // Arrange: token emitido 2 horas atrás, com validade de 1 hora
        var twoHoursAgo = Clock.fixed(Instant.now().minus(Duration.ofHours(2)), ZoneOffset.UTC);
        var oldService = new JwtService(jwtEncoder, jwtDecoder, jwtProperties, twoHoursAgo);
        var expired = oldService.issue(user(7L, Role.ADMIN)).value();

        // Act
        var result = mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired));

        // Assert
        result.andExpect(status().isUnauthorized());
        verifyNoInteractions(userService);
    }

    @Test
    void shouldReturnOwnDataWithValidToken() throws Exception {
        // Arrange
        when(userService.findById(7L)).thenReturn(new UserResponse(7L, "Pedro", "pedro@atlasroute.com", Role.DRIVER));

        // Act
        var result = mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(7L, Role.DRIVER)));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.role").value("DRIVER"));
    }

    @Test
    void shouldReturn403ProblemDetailWhenDriverListsUsers() throws Exception {
        // Arrange
        var driverToken = bearer(7L, Role.DRIVER);

        // Act
        var result = mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, driverToken));

        // Assert: o @PreAuthorize nega e o handler devolve 403, não 500
        result.andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Acesso negado"))
                .andExpect(jsonPath("$.detail").value(ProblemDetailSecurityHandler.FORBIDDEN));
        verifyNoInteractions(userService);
    }

    @Test
    void shouldListUsersWhenAdmin() throws Exception {
        // Arrange
        when(userService.list(any())).thenReturn(new PageImpl<>(
                List.of(new UserResponse(1L, "Admin", "admin@atlasroute.com", Role.ADMIN)),
                PageRequest.of(0, 20), 1));

        // Act
        var result = mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, bearer(1L, Role.ADMIN)));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("admin@atlasroute.com"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    private String bearer(Long id, Role role) {
        return "Bearer " + jwtService.issue(user(id, role)).value();
    }

    private static User user(Long id, Role role) {
        var user = new User("Pedro", "pedro@atlasroute.com", "hash", role, Instant.now());
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
