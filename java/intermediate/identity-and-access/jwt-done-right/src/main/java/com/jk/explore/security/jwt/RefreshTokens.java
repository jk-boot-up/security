package com.jk.explore.security.jwt;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Refresh tokens kept on the server. An access token cannot be taken back before it expires; a refresh token can, because the server
 * looks it up every time. Handles here are counters, for a repeatable demo; real handles are long random values.
 */
final class RefreshTokens {

    private final Map<String, String> subjects = new HashMap<>();
    private int next = 1;

    String issue(String subject) {
        String handle = "rt-" + next++;
        subjects.put(handle, subject);
        return handle;
    }

    void revokeAll(String subject) {
        subjects.values().removeIf(subject::equals);
    }

    Optional<String> redeem(String handle) {
        return Optional.ofNullable(subjects.get(handle));
    }
}
