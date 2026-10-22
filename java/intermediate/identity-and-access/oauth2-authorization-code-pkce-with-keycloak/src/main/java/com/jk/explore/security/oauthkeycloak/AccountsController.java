package com.jk.explore.security.oauthkeycloak;

import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AccountsController {

    static final String ANA = "a0a0a0a0-0000-4000-8000-000000000001";
    static final String BEN = "b0b0b0b0-0000-4000-8000-000000000002";

    private static final Map<String, List<String>> ACCOUNTS = Map.of(ANA, List.of("current", "savings"), BEN, List.of("current"));

    @GetMapping("/accounts")
    Map<String, Object> accounts(@AuthenticationPrincipal Jwt token) {
        List<String> accounts = ACCOUNTS.getOrDefault(token.getSubject(), List.of());
        return Map.of("owner", token.getSubject().equals(ANA) ? "ana" : "ben", "accounts", accounts.size());
    }

    @PostMapping("/payments")
    Map<String, Object> pay() {
        return Map.of("paid", true);
    }
}
