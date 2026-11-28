package com.atlasroute.api.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Corpo do {@code POST /api/v1/auth/login}. Sem {@code @Email} de propósito: um e-mail mal
 * formatado simplesmente não encontra usuário e recebe o mesmo 401 genérico.
 */
public record LoginRequest(
        @NotBlank(message = "é obrigatório")
        String email,

        @NotBlank(message = "é obrigatória")
        String password
) {

    public LoginRequest {
        email = email == null ? null : email.strip();
    }

    /** Evita que a senha apareça em logs se o objeto for impresso. */
    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=***]";
    }
}
