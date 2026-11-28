package com.atlasroute.api.security;

import com.atlasroute.api.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Rotas públicas que só existem com a aplicação inteira (Swagger e Actuator). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PublicRoutesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldExposeOpenApiWithBearerSchemeWithoutToken() throws Exception {
        // Arrange
        var docs = "/v3/api-docs";

        // Act
        var result = mockMvc.perform(get(docs));

        // Assert: o botão "Authorize" do Swagger vem deste esquema
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"));
    }

    @Test
    void shouldExposeSwaggerUiAndHealthWithoutToken() throws Exception {
        // Arrange
        var swaggerUi = "/swagger-ui/index.html";
        var health = "/actuator/health";

        // Act
        var ui = mockMvc.perform(get(swaggerUi));
        var healthResult = mockMvc.perform(get(health));

        // Assert
        ui.andExpect(status().isOk()).andExpect(content().string(containsString("Swagger UI")));
        healthResult.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void shouldProtectActuatorEndpointsOtherThanHealth() throws Exception {
        // Arrange
        var info = "/actuator/info";

        // Act
        var result = mockMvc.perform(get(info));

        // Assert
        result.andExpect(status().isUnauthorized());
    }
}
