package com.jk.explore.security.tlscheck;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Turns a server profile into findings and a grade. Every rule has an id, so a build can fail on "no-hsts" without caring about the text. */
public final class Checker {

    private static final Set<String> OLD_PROTOCOLS = Set.of("SSLv2", "SSLv3", "TLSv1", "TLSv1.1");
    private static final long SIX_MONTHS = 180L * 24 * 3600;

    public Report check(ServerProfile s, Instant now) {
        List<Finding> f = new ArrayList<>();
        protocols(s, f);
        ciphers(s, f);
        certificate(s, now, f);
        http(s, f);
        if (s.compression()) {
            f.add(new Finding("tls-compression", Severity.HIGH, "TLS compression is on (CRIME)"));
        }
        if (!s.secureRenegotiation()) {
            f.add(new Finding("insecure-renegotiation", Severity.HIGH, "secure renegotiation is not supported"));
        }
        f.sort(Comparator.comparing(Finding::severity).reversed().thenComparing(Finding::rule));
        int score = Math.max(0, 100 - f.stream().mapToInt(x -> x.severity().penalty()).sum());
        return new Report(s.host(), List.copyOf(f), score, grade(score, f));
    }

    private static void protocols(ServerProfile s, List<Finding> f) {
        for (String p : OLD_PROTOCOLS) {
            if (s.protocols().contains(p)) {
                f.add(new Finding("old-protocol-" + p.toLowerCase(Locale.ROOT).replace(".", "_"), p.startsWith("SSL") ? Severity.CRITICAL : Severity.HIGH, p + " is enabled"));
            }
        }
        if (!s.protocols().contains("TLSv1.2") && !s.protocols().contains("TLSv1.3")) {
            f.add(new Finding("no-modern-protocol", Severity.CRITICAL, "neither TLS 1.2 nor TLS 1.3 is offered"));
        } else if (!s.protocols().contains("TLSv1.3")) {
            f.add(new Finding("no-tls13", Severity.LOW, "TLS 1.3 is not offered"));
        }
    }

    private static void ciphers(ServerProfile s, List<Finding> f) {
        List<String> broken = s.cipherSuites().stream().filter(CipherSuites::broken).toList();
        if (!broken.isEmpty()) {
            f.add(new Finding("broken-cipher", Severity.HIGH, broken.size() + " broken suite(s): " + String.join(", ", broken)));
        }
        List<String> noFs = s.cipherSuites().stream().filter(c -> !CipherSuites.broken(c) && CipherSuites.lacksForwardSecrecy(c)).toList();
        if (!noFs.isEmpty()) {
            f.add(new Finding("no-forward-secrecy", Severity.MEDIUM, noFs.size() + " suite(s) without forward secrecy: " + String.join(", ", noFs)));
        }
        if (s.cipherSuites().stream().anyMatch(c -> !CipherSuites.broken(c) && CipherSuites.cbc(c) && !CipherSuites.lacksForwardSecrecy(c))) {
            f.add(new Finding("cbc-suites", Severity.LOW, "CBC suites are offered next to GCM or ChaCha20"));
        }
        if (!s.serverChoosesCipher() && !s.cipherSuites().stream().allMatch(c -> CipherSuites.tls13(c) || (!CipherSuites.broken(c) && !CipherSuites.lacksForwardSecrecy(c)))) {
            f.add(new Finding("client-chooses-cipher", Severity.LOW, "the server lets the client pick among weak suites"));
        }
    }

    private static void certificate(ServerProfile s, Instant now, List<Finding> f) {
        CertFacts c = s.certificate();
        if (now.isAfter(c.notAfter())) {
            f.add(new Finding("cert-expired", Severity.CRITICAL, "certificate expired " + Duration.between(c.notAfter(), now).toDays() + " day(s) ago"));
        } else if (now.isBefore(c.notBefore())) {
            f.add(new Finding("cert-not-yet-valid", Severity.CRITICAL, "certificate is not valid yet"));
        } else if (Duration.between(now, c.notAfter()).toDays() < 14) {
            f.add(new Finding("cert-expires-soon", Severity.MEDIUM, "certificate expires in " + Duration.between(now, c.notAfter()).toDays() + " day(s)"));
        }
        if (c.dnsNames().stream().noneMatch(n -> nameMatches(n, s.host()))) {
            f.add(new Finding("name-mismatch", Severity.CRITICAL, s.host() + " is not in " + c.dnsNames()));
        }
        if (c.selfSigned() || !c.trustedRoot()) {
            f.add(new Finding("untrusted-chain", Severity.CRITICAL, c.selfSigned() ? "self-signed certificate" : "chain does not end at a trusted root"));
        } else if (!c.chainComplete()) {
            f.add(new Finding("incomplete-chain", Severity.HIGH, "the server does not send its intermediate certificate"));
        }
        if (c.keyType().equals("RSA") && c.keyBits() < 2048) {
            f.add(new Finding("weak-key", Severity.HIGH, "RSA key of " + c.keyBits() + " bits"));
        }
        if (c.keyType().equals("EC") && c.keyBits() < 256) {
            f.add(new Finding("weak-key", Severity.HIGH, "EC key of " + c.keyBits() + " bits"));
        }
        if (c.signature().contains("MD5") || c.signature().contains("SHA1")) {
            f.add(new Finding("weak-signature", Severity.HIGH, "certificate signed with " + c.signature()));
        }
    }

    private static void http(ServerProfile s, List<Finding> f) {
        HttpFacts h = s.http();
        if (h.hstsMaxAgeSeconds() < 0) {
            f.add(new Finding("no-hsts", Severity.MEDIUM, "no Strict-Transport-Security header"));
        } else if (h.hstsMaxAgeSeconds() < SIX_MONTHS) {
            f.add(new Finding("short-hsts", Severity.LOW, "HSTS max-age is only " + h.hstsMaxAgeSeconds() / 86400 + " days"));
        } else if (!h.hstsIncludeSubDomains()) {
            f.add(new Finding("hsts-no-subdomains", Severity.INFO, "HSTS does not include subdomains"));
        }
        if (!h.httpRedirectsToHttps()) {
            f.add(new Finding("no-https-redirect", Severity.MEDIUM, "plain HTTP does not redirect to HTTPS"));
        }
        if (!h.sessionCookieSecure()) {
            f.add(new Finding("cookie-not-secure", Severity.MEDIUM, "the session cookie lacks the Secure attribute"));
        }
    }

    /** Exact match or one left-most wildcard label. */
    static boolean nameMatches(String pattern, String host) {
        String p = pattern.toLowerCase(Locale.ROOT);
        String h = host.toLowerCase(Locale.ROOT);
        if (p.equals(h)) {
            return true;
        }
        return p.startsWith("*.") && h.indexOf('.') > 0 && h.substring(h.indexOf('.')).equals(p.substring(1));
    }

    private static char grade(int score, List<Finding> f) {
        if (f.stream().anyMatch(x -> x.severity() == Severity.CRITICAL)) {
            return 'F';
        }
        char g = score >= 90 ? 'A' : score >= 75 ? 'B' : score >= 60 ? 'C' : score >= 40 ? 'D' : 'F';
        if (f.stream().anyMatch(x -> x.severity() == Severity.HIGH) && g < 'C') {
            return 'C'; // a high finding caps the grade at C, whatever the score
        }
        return g;
    }
}
