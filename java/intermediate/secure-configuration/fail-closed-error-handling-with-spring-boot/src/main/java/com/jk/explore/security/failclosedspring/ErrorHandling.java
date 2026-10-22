package com.jk.explore.security.failclosedspring;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * One place that turns every failure into the same short answer. The customer gets a reference; the log gets the reason and no card number.
 * It is switched off in the {@code leaky} profile, which shows what Spring Boot's own error response carries when its debugging settings are on.
 */
@RestControllerAdvice
@Profile("!leaky")
class ErrorHandling {

    static final String CARD = "bank.card";

    private final SecurityLog log;
    private final References references;

    ErrorHandling(SecurityLog log, References references) {
        this.log = log;
        this.references = references;
    }

    @ExceptionHandler(CheckFailedException.class)
    ProblemDetail checkFailed(CheckFailedException e, WebRequest request) {
        return answer(HttpStatus.SERVICE_UNAVAILABLE, e.why, request);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail anythingElse(Exception e, WebRequest request) {
        return answer(HttpStatus.INTERNAL_SERVER_ERROR, "unexpected " + e.getClass().getSimpleName(), request);
    }

    private ProblemDetail answer(HttpStatus status, String why, WebRequest request) {
        String reference = references.next();
        Object card = request.getAttribute(CARD, WebRequest.SCOPE_REQUEST);
        log.refusal(reference, why, card == null ? "" : card.toString());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "The payment could not be completed. Please try again later, quoting reference " + reference + ".");
        problem.setTitle("Payment not completed");
        return problem;
    }
}
