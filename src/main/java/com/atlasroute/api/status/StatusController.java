package com.atlasroute.api.status;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/status")
@Tag(name = "Status", description = "Verificação rápida de que a API está no ar")
public class StatusController {

    private final String appName;
    private final Clock clock;

    public StatusController(@Value("${spring.application.name}") String appName, Clock clock) {
        this.appName = appName;
        this.clock = clock;
    }

    @GetMapping
    @Operation(summary = "Retorna o status da API")
    public StatusResponse status() {
        return new StatusResponse(appName, "UP", Instant.now(clock));
    }

    public record StatusResponse(String app, String status, Instant timestamp) {
    }
}
