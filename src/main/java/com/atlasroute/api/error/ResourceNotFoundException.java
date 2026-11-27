package com.atlasroute.api.error;

/**
 * Lançada pelos services quando um recurso pedido não existe (ex.: veículo com id inexistente).
 * O {@link GlobalExceptionHandler} transforma em 404 com ProblemDetail.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, Object id) {
        this(resource + " com id " + id + " não encontrado(a).");
    }
}
