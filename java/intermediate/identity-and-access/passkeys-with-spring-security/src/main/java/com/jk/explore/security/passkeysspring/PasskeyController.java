package com.jk.explore.security.passkeysspring;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class PasskeyController {

    /** The CSRF token for the page's script, which sends it back in a header on every post. */
    @GetMapping("/csrf")
    Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken());
    }

    @GetMapping("/me")
    Map<String, String> me(Authentication who) {
        Map<String, String> me = new LinkedHashMap<>();
        me.put("name", who.getName());
        me.put("signedInWith", who.getClass().getSimpleName());
        return me;
    }
}
