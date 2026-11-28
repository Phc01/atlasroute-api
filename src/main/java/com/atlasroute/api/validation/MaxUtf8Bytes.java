package com.atlasroute.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Limita o tamanho de um texto em bytes UTF-8, não em caracteres.
 *
 * <p>Existe por causa do BCrypt, que só considera os primeiros 72 bytes da senha: uma letra
 * com acento ocupa 2 bytes e um emoji 4, então {@code @Size(max = 72)} não basta.
 * {@code null} é considerado válido (use {@code @NotBlank} junto).
 */
@Documented
@Constraint(validatedBy = MaxUtf8BytesValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxUtf8Bytes {

    int value();

    String message() default "deve ter no máximo {value} bytes";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
