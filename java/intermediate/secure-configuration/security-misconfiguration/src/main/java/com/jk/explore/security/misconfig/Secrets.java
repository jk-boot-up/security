package com.jk.explore.security.misconfig;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;

/** Password comparison in constant time, and the list of defaults that attackers try first. */
final class Secrets {

    /** Passwords that products ship with and that every scanner tries. */
    static final List<String> KNOWN_DEFAULTS = List.of("", "admin", "password", "changeme", "123456", "root", "secret");

    private Secrets() {
    }

    static boolean same(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    static boolean isKnownDefault(String password) {
        String lower = password.strip().toLowerCase(Locale.ROOT);
        return KNOWN_DEFAULTS.contains(lower) || lower.length() < 12;
    }
}
