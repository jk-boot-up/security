package com.jk.explore.security.apispring;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

/** Short, fixed answers for a bad request: they say what was wrong, never what the server is. */
@RestControllerAdvice
@Profile("safe")
class ErrorHandling {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail unreadable(HttpMessageNotReadableException e) {
        boolean unknown = e.getCause() instanceof UnrecognizedPropertyException;
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, unknown ? "unknown field" : "unreadable body");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalid(MethodArgumentNotValidException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "invalid " + e.getBindingResult().getFieldErrors().stream().map(f -> f.getField()).sorted().findFirst().orElse("body"));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ProblemDetail invalidParameter(HandlerMethodValidationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "invalid parameter");
    }
}
