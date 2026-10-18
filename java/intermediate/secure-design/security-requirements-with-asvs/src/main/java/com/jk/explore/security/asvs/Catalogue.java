package com.jk.explore.security.asvs;

import java.util.List;

/**
 * The store's and the bank's requirements. Chapters are named as in ASVS 5.0; the statements are the team's own short paraphrases, not the
 * standard's text, and the ids are the team's own.
 */
final class Catalogue {

    static final String EDITION = "5.0";

    private Catalogue() {
    }

    static List<Requirement> all() {
        return List.of(
                new Requirement("SR-01", "Web Frontend Security", Level.L1, Verification.AUTOMATED, "headers", EDITION,
                        "Every page sets a content security policy, nosniff and a framing policy"),
                new Requirement("SR-02", "Session Management", Level.L1, Verification.AUTOMATED, "cookies", EDITION,
                        "The session cookie is Secure and HttpOnly"),
                new Requirement("SR-03", "Authentication", Level.L1, Verification.AUTOMATED, "password-storage", EDITION,
                        "Passwords are stored with a slow, salted hash"),
                new Requirement("SR-04", "Session Management", Level.L1, Verification.AUTOMATED, "session-rotation", EDITION,
                        "A new session id is issued at sign-in"),
                new Requirement("SR-05", "Security Logging and Error Handling", Level.L1, Verification.AUTOMATED, "error-leak", EDITION,
                        "Error pages show no stack traces or internal details"),
                new Requirement("SR-06", "Configuration", Level.L1, Verification.CONFIGURATION, "debug-off", EDITION,
                        "Debug features are switched off in production"),
                new Requirement("SR-07", "Secure Communication", Level.L1, Verification.CONFIGURATION, "tls", EDITION,
                        "All traffic uses TLS 1.2 or later"),
                new Requirement("SR-08", "Authorization", Level.L1, Verification.MANUAL, "", EDITION,
                        "Access to every order and account is checked on the server"),
                new Requirement("SR-09", "Authentication", Level.L2, Verification.CONFIGURATION, "staff-mfa", EDITION,
                        "Staff accounts sign in with a second factor"),
                new Requirement("SR-10", "Validation and Business Logic", Level.L2, Verification.MANUAL, "", EDITION,
                        "Business limits, such as refunds per day, are enforced on the server"),
                new Requirement("SR-11", "Cryptography", Level.L3, Verification.MANUAL, "", EDITION,
                        "Signing keys are held in a hardware module or a key service"),
                new Requirement("SR-12", "Security Logging and Error Handling", Level.L2, Verification.CONFIGURATION, "audit-log", EDITION,
                        "Security events are logged with who, what and when"),
                new Requirement("SR-13", "Data Protection", Level.L3, Verification.AUTOMATED, "no-store", EDITION,
                        "Account pages are marked so that browsers and proxies do not cache them"),
                new Requirement("SR-14", "Secure Coding and Architecture", Level.L2, Verification.MANUAL, "", "4.0.3",
                        "Third-party components are listed and kept up to date"));
    }
}
