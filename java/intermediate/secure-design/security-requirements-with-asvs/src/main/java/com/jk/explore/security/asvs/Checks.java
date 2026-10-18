package com.jk.explore.security.asvs;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** The automated and configuration checks, by name. Each one asks the running store a question and answers pass or fail. */
final class Checks {

    record Check(String name, String question, Predicate<SampleApp> passes) {
    }

    /** The lowest work factor accepted for PBKDF2 with HMAC-SHA-256, as OWASP's password storage guidance recommends. */
    static final int PBKDF2_MINIMUM_ROUNDS = 600_000;

    private static final Set<String> MODERN_TLS = Set.of("TLSv1.2", "TLSv1.3");
    private static final char[] SAMPLE_PASSWORD = "correct horse battery staple".toCharArray();

    private static final Map<String, Check> ALL = new LinkedHashMap<>();

    static {
        add("headers", "does a page carry a CSP, nosniff and a framing policy", app -> {
            Map<String, String> h = app.responseHeaders("/products");
            return h.containsKey("Content-Security-Policy") && "nosniff".equals(h.get("X-Content-Type-Options"))
                    && (h.containsKey("X-Frame-Options") || h.get("Content-Security-Policy").contains("frame-ancestors"));
        });
        add("cookies", "is the session cookie Secure and HttpOnly", app -> {
            String cookie = app.sessionCookie(SampleApp.newSessionId());
            return cookie.contains("; Secure") && cookie.contains("; HttpOnly");
        });
        add("password-storage", "is a password kept as a slow, salted hash", app -> {
            String[] parts = app.storedPasswordFor(SAMPLE_PASSWORD).split("\\$");
            return parts.length == 4 && parts[0].equals("pbkdf2-sha256") && Integer.parseInt(parts[1]) >= PBKDF2_MINIMUM_ROUNDS;
        });
        add("session-rotation", "does sign-in issue a new session id", app -> {
            String before = SampleApp.newSessionId();
            return !app.signIn(before).equals(before);
        });
        add("error-leak", "does an error page hide the stack trace", app -> {
            String page = app.errorPage(new IllegalStateException("order database unavailable"));
            return !page.contains("\tat ") && !page.contains("IllegalStateException") && !page.contains("database");
        });
        add("debug-off", "is the debug endpoint closed", app -> !app.debugEndpointOpen());
        add("tls", "is the oldest TLS version allowed 1.2 or later", app -> MODERN_TLS.contains(app.settings().tlsMinimum()));
        add("staff-mfa", "do staff sign in with a second factor", app -> app.settings().staffMfa());
        add("audit-log", "are security events logged", app -> app.settings().auditLog());
        add("no-store", "are account pages marked no-store", app -> "no-store".equals(app.responseHeaders("/account").get("Cache-Control")));
    }

    private Checks() {
    }

    private static void add(String name, String question, Predicate<SampleApp> passes) {
        ALL.put(name, new Check(name, question, passes));
    }

    static Map<String, Check> all() {
        return ALL;
    }

    static Check named(String name) {
        Check check = ALL.get(name);
        if (check == null) {
            throw new IllegalArgumentException("no check named " + name);
        }
        return check;
    }
}
