package com.atlasroute.api.error;

/**
 * Lançada quando a requisição entra em conflito com o estado atual (ex.: e-mail já cadastrado).
 * O {@link GlobalExceptionHandler} transforma em 409 com ProblemDetail.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
