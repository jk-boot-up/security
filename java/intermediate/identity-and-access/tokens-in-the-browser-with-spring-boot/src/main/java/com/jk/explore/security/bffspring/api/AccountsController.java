package com.jk.explore.security.bffspring.api;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AccountsController {

    private final Ledger ledger;

    AccountsController(Ledger ledger) {
        this.ledger = ledger;
    }

    @GetMapping("/accounts")
    Map<String, Object> accounts(@AuthenticationPrincipal Jwt token) {
        return Map.of("owner", token.getSubject(), "balanceCents", 125_000);
    }

    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, Object> transfer(@AuthenticationPrincipal Jwt token) {
        ledger.add("transfer by " + token.getSubject());
        return Map.of("transfers", ledger.transfers().size());
    }
}
