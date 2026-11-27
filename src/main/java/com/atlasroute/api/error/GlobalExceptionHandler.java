package com.atlasroute.api.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Tratamento centralizado de erros no formato RFC 9457 (ProblemDetail).
 *
 * <p>Estende {@link ResponseEntityExceptionHandler}, então as exceções padrão do Spring MVC
 * (método não suportado, media type inválido etc.) já saem como ProblemDetail. Aqui só
 * sobrescrevemos as que precisam de mensagem em português ou de campos extras.
 *
 * <p>Toda resposta de erro ganha o campo {@code timestamp}; o {@code instance} (caminho da
 * requisição) é preenchido automaticamente pelo Spring.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    /** Campo inválido em um corpo anotado com {@code @Valid}. */
    public record FieldViolation(String field, String message) {
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        List<FieldViolation> errors = new ArrayList<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errors.add(new FieldViolation(e.getField(), e.getDefaultMessage())));
        ex.getBindingResult().getGlobalErrors()
                .forEach(e -> errors.add(new FieldViolation(e.getObjectName(), e.getDefaultMessage())));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "Um ou mais campos estão inválidos.");
        problem.setTitle("Dados inválidos");
        problem.setProperty("errors", errors);

        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                status, "O corpo da requisição está ausente ou mal formatado.");
        problem.setTitle("Requisição inválida");

        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(
            NoResourceFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "Rota não encontrada.");
        problem.setTitle("Não encontrado");

        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Não encontrado");

        return handleExceptionInternal(ex, problem, new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }

    /** Última barreira: nunca expõe a mensagem interna da exceção para o cliente. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpectedError(Exception ex, WebRequest request) {
        log.error("Erro inesperado ao processar {}", request.getDescription(false), ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro inesperado. Tente novamente mais tarde.");
        problem.setTitle("Erro interno");

        return handleExceptionInternal(ex, problem, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    /** Ponto por onde passam todas as respostas de erro: adiciona o timestamp. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {

        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response != null && response.getBody() instanceof ProblemDetail problem) {
            problem.setProperty("timestamp", Instant.now(clock));
        }
        return response;
    }
}
