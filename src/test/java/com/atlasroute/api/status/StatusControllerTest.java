package com.atlasroute.api.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatusController.class)
class StatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private Clock clock;

    @Test
    void deveRetornarStatusUp() throws Exception {
        // Arrange
        when(clock.instant()).thenReturn(Instant.parse("2026-11-26T12:00:00Z"));

        // Act
        var resultado = mockMvc.perform(get("/api/v1/status"));

        // Assert
        resultado.andExpect(status().isOk())
                .andExpect(jsonPath("$.app").value("atlasroute-api"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.timestamp").value("2026-11-26T12:00:00Z"));
    }
}
