package com.atlasroute.api.auth;

import com.atlasroute.api.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.NotBlank;

/**
 * Corpo do {@code POST /api/v1/auth/login}. Sem {@code @Email} de propósito: um e-mail mal
 * formatado simplesmente não encontra usuário e recebe o mesmo 401 genérico.
 *
 * <p>A senha tem o mesmo teto de 72 bytes do cadastro: nenhuma senha cadastrada passa disso, e
 * o BCrypt recusaria uma maior (o que viraria 500 em vez de uma resposta clara).
 */
public record LoginRequest(
        @NotBlank(message = "é obrigatório")
        String email,

        @NotBlank(message = "é obrigatória")
        @MaxUtf8Bytes(value = 72, message = "deve ter no máximo 72 bytes")
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
