package com.jk.explore.security.tokens;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** The authorization server's token table: opaque random tokens, each for one user and optionally bound to one key's thumbprint. */
public final class AuthServer {

    /** What a token stands for. {@code thumbprint} is null for a plain bearer token. */
    public record Grant(String user, String thumbprint) {
    }

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Grant> tokens = new HashMap<>();

    public String issue(String user, String thumbprint) {
        byte[] b = new byte[24];
        random.nextBytes(b);
        String token = B64.encode(b);
        tokens.put(token, new Grant(user, thumbprint));
        return token;
    }

    public Optional<Grant> lookup(String token) {
        return Optional.ofNullable(tokens.get(token));
    }
}
