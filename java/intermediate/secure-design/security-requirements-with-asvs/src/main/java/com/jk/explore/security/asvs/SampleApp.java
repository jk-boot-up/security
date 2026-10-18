package com.jk.explore.security.asvs;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * A small online store that runs in the same process, so the checks can ask it questions: what headers a page carries, what the session
 * cookie looks like, how a password is stored, whether sign-in issues a new session id, and what an error page shows. Its behaviour comes
 * entirely from its settings, which is what a regression changes.
 */
final class SampleApp {

    /** The store's settings, as they would sit in its production configuration. */
    record Settings(boolean contentSecurityPolicy, boolean noSniff, boolean framePolicy, boolean cookieSecure, boolean cookieHttpOnly,
                    String passwordScheme, int passwordRounds, boolean rotateSessionAtSignIn, boolean debug, String tlsMinimum,
                    boolean staffMfa, boolean auditLog, boolean noStoreOnAccountPages) {

        /** The store as it is configured today. */
        static Settings store() {
            return new Settings(true, true, true, true, true, "pbkdf2-sha256", 600_000, true, false, "TLSv1.2", false, true, true);
        }

        Settings withDebug(boolean on) {
            return new Settings(contentSecurityPolicy, noSniff, framePolicy, cookieSecure, cookieHttpOnly, passwordScheme, passwordRounds,
                    rotateSessionAtSignIn, on, tlsMinimum, staffMfa, auditLog, noStoreOnAccountPages);
        }

        Settings withStaffMfa(boolean on) {
            return new Settings(contentSecurityPolicy, noSniff, framePolicy, cookieSecure, cookieHttpOnly, passwordScheme, passwordRounds,
                    rotateSessionAtSignIn, debug, tlsMinimum, on, auditLog, noStoreOnAccountPages);
        }

        Settings withFastPasswordHash() {
            return new Settings(contentSecurityPolicy, noSniff, framePolicy, cookieSecure, cookieHttpOnly, "sha256", 1,
                    rotateSessionAtSignIn, debug, tlsMinimum, staffMfa, auditLog, noStoreOnAccountPages);
        }
    }

    private static final SecureRandom RANDOM = new SecureRandom();

    private final Settings settings;
    private String storedPassword;
    private int errors;

    SampleApp(Settings settings) {
        this.settings = settings;
    }

    Settings settings() {
        return settings;
    }

    Map<String, String> responseHeaders(String path) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "text/html; charset=utf-8");
        if (settings.contentSecurityPolicy()) {
            headers.put("Content-Security-Policy", "default-src 'self'; frame-ancestors 'none'");
        }
        if (settings.noSniff()) {
            headers.put("X-Content-Type-Options", "nosniff");
        }
        if (settings.framePolicy()) {
            headers.put("X-Frame-Options", "DENY");
        }
        if (settings.noStoreOnAccountPages() && path.startsWith("/account")) {
            headers.put("Cache-Control", "no-store");
        }
        return headers;
    }

    String sessionCookie(String id) {
        StringBuilder cookie = new StringBuilder("session=").append(id).append("; Path=/; SameSite=Lax");
        if (settings.cookieSecure()) {
            cookie.append("; Secure");
        }
        if (settings.cookieHttpOnly()) {
            cookie.append("; HttpOnly");
        }
        return cookie.toString();
    }

    /** What the store keeps for a customer's password. Computed once, because a slow hash is meant to be slow. */
    String storedPasswordFor(char[] password) {
        if (storedPassword == null) {
            storedPassword = hash(password);
        }
        return storedPassword;
    }

    private String hash(char[] password) {
        try {
            if (settings.passwordScheme().equals("pbkdf2-sha256")) {
                byte[] salt = new byte[16];
                RANDOM.nextBytes(salt);
                PBEKeySpec spec = new PBEKeySpec(password, salt, settings.passwordRounds(), 256);
                byte[] derived = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
                spec.clearPassword();
                Base64.Encoder b64 = Base64.getEncoder().withoutPadding();
                return "pbkdf2-sha256$" + settings.passwordRounds() + "$" + b64.encodeToString(salt) + "$" + b64.encodeToString(derived);
            }
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(new String(password).getBytes(StandardCharsets.UTF_8));
            return "sha256$" + HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("the JDK provides PBKDF2 and SHA-256", e);
        }
    }

    static String newSessionId() {
        byte[] id = new byte[16];
        RANDOM.nextBytes(id);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(id);
    }

    /** Signs a customer in. With rotation on, the session id held before sign-in is replaced. */
    String signIn(String sessionBefore) {
        return settings.rotateSessionAtSignIn() ? newSessionId() : sessionBefore;
    }

    String errorPage(Exception failure) {
        errors++;
        if (settings.debug()) {
            StringBuilder page = new StringBuilder("<h1>").append(failure).append("</h1><pre>");
            for (StackTraceElement frame : failure.getStackTrace()) {
                page.append("\tat ").append(frame).append('\n');
            }
            return page.append("</pre>").toString();
        }
        return "<h1>Something went wrong</h1><p>Reference E-" + errors + "</p>";
    }

    boolean debugEndpointOpen() {
        return settings.debug();
    }
}
