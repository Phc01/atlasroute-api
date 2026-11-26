package com.atlasroute.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Expor o Clock como bean deixa qualquer código que dependa de data/hora testável
 * (nos testes trocamos por um Clock fixo).
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
