package com.jk.explore.security.oidcspring;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class StoreController {

    private final StoreAccounts accounts;

    StoreController(StoreAccounts accounts) {
        this.accounts = accounts;
    }

    @GetMapping("/")
    String home() {
        return "the store";
    }

    /** Who is signed in: the issuer and the subject, and the names of the claims in the ID token. */
    @GetMapping("/me")
    Map<String, Object> me(@AuthenticationPrincipal OidcUser user) {
        Map<String, Object> me = new LinkedHashMap<>();
        me.put("issuer", user.getIssuer().toString());
        me.put("subject", user.getSubject());
        me.put("email", user.getEmail());
        me.put("emailVerified", user.getEmailVerified());
        me.put("sid", user.getClaimAsString("sid"));
        me.put("nonce", user.getClaimAsString("nonce"));
        me.put("claims", new TreeSet<>(user.getIdToken().getClaims().keySet()));
        return me;
    }

    /** Which store account this shopper gets, under the two ways of keying accounts. */
    @GetMapping("/account")
    Map<String, String> account(@AuthenticationPrincipal OidcUser user, @RequestParam String keyedBy) {
        String account = keyedBy.equals("email") ? accounts.byEmail(user.getEmail()) : accounts.bySubject(user.getIssuer().toString(), user.getSubject());
        return Map.of("account", account);
    }

    @GetMapping("/csrf")
    Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken());
    }
}
