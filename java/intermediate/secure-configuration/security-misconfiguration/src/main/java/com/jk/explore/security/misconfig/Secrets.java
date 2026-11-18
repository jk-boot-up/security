package com.jk.explore.security.misconfig;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;

/** Password comparison in constant time, and the rules a production admin password must meet. */
final class Secrets {

    /** Passwords that products ship with and that every scanner tries. */
    static final List<String> KNOWN_DEFAULTS = List.of("admin", "password", "changeme", "123456", "root", "secret");

    /** The shortest admin password the startup gate accepts in production. */
    static final int MIN_LENGTH = 12;

    private Secrets() {
    }

    static boolean same(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    /** Why a password is too weak for production, or an empty string when it is strong enough. */
    static String weakness(String password) {
        String lower = password.strip().toLowerCase(Locale.ROOT);
        if (lower.isEmpty()) {
            return "is empty";
        }
        if (KNOWN_DEFAULTS.contains(lower)) {
            return "is a known default";
        }
        if (lower.length() < MIN_LENGTH) {
            return "has " + lower.length() + " characters, under " + MIN_LENGTH;
        }
        return "";
    }
}
