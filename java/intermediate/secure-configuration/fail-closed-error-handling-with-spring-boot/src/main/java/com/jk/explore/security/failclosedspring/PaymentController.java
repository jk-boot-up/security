package com.jk.explore.security.failclosedspring;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class PaymentController {

    private final PaymentService payments;
    private final SecurityLog log;
    private final References references;

    PaymentController(PaymentService payments, SecurityLog log, References references) {
        this.payments = payments;
        this.log = log;
        this.references = references;
    }

    @PostMapping("/payments")
    ResponseEntity<Decision> pay(@RequestBody Payment payment, HttpServletRequest request) {
        request.setAttribute(ErrorHandling.CARD, payment.card());   // so the error handler can log the last four digits
        Decision decision = payments.decide(payment);
        if (!decision.approved()) {
            log.refusal(references.next(), decision.reason(), payment.card());
        }
        return ResponseEntity.status(decision.approved() ? HttpStatus.OK : HttpStatus.FORBIDDEN).body(decision);
    }
}
