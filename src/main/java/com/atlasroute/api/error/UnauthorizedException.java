package com.atlasroute.api.error;

/**
 * Lançada quando a requisição não pode ser autenticada (ex.: e-mail ou senha errados no login).
 * O {@link GlobalExceptionHandler} transforma em 401 com ProblemDetail.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
