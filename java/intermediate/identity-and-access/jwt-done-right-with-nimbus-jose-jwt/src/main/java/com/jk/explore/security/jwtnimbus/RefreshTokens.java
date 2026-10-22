package com.jk.explore.security.jwtnimbus;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/** Refresh tokens are not JWTs: they are random strings that the server looks up, so it can take one back at any moment. */
final class RefreshTokens {

    private final SecureRandom random = new SecureRandom();
    private final Map<String, String> owners = new HashMap<>();

    String issue(String subject) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        owners.put(token, subject);
        return token;
    }

    void revokeAll(String subject) {
        owners.values().removeIf(subject::equals);
    }

    /** The subject, or {@code null} when the token is unknown or was revoked. */
    String lookup(String token) {
        return owners.get(token);
    }
}
