package com.atlasroute.api.auth;

import com.atlasroute.api.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo do {@code POST /api/v1/auth/register}.
 *
 * <p>O construtor compacto tira os espaços das pontas do nome e do e-mail antes da validação,
 * então " pedro@x.com " passa no {@code @Email}. A senha não é alterada.
 * O limite da senha é de 72 <b>bytes</b> (não caracteres), o máximo que o BCrypt aceita.
 */
public record RegisterRequest(
        @NotBlank(message = "é obrigatório")
        @Size(max = 120, message = "deve ter no máximo 120 caracteres")
        String name,

        @NotBlank(message = "é obrigatório")
        @Email(message = "deve ser um e-mail válido")
        @Size(max = 255, message = "deve ter no máximo 255 caracteres")
        String email,

        @NotBlank(message = "é obrigatória")
        @Size(min = 8, message = "deve ter no mínimo 8 caracteres")
        @MaxUtf8Bytes(value = 72, message = "deve ter no máximo 72 bytes (letras com acento e emojis contam mais de 1)")
        String password
) {

    public RegisterRequest {
        name = name == null ? null : name.strip();
        email = email == null ? null : email.strip();
    }

    /** Evita que a senha apareça em logs se o objeto for impresso. */
    @Override
    public String toString() {
        return "RegisterRequest[name=" + name + ", email=" + email + ", password=***]";
    }
}
