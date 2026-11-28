package com.atlasroute.api.auth;

/**
 * Resposta do login.
 *
 * @param accessToken JWT para mandar em {@code Authorization: Bearer <token>}
 * @param tokenType   sempre {@code Bearer}
 * @param expiresIn   validade do token, em segundos
 */
public record LoginResponse(String accessToken, String tokenType, long expiresIn) {

    public static LoginResponse bearer(String accessToken, long expiresIn) {
        return new LoginResponse(accessToken, "Bearer", expiresIn);
    }
}
