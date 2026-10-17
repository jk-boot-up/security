package com.jk.explore.security.httpnginx;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Scores a response against five security headers, one point each. */
public final class HeaderAudit {

    public record Finding(String header, boolean passed, String reason) {
    }

    private static final Set<String> SAFE_REFERRER = Set.of("no-referrer", "same-origin", "strict-origin", "strict-origin-when-cross-origin", "origin-when-cross-origin");

    private HeaderAudit() {
    }

    public static List<Finding> audit(Map<String, String> responseHeaders) {
        Map<String, String> h = new TreeMap<>();
        responseHeaders.forEach((k, v) -> h.put(k.toLowerCase(Locale.ROOT), v));
        List<Finding> findings = new ArrayList<>();
        findings.add(hsts(h.get("strict-transport-security")));
        findings.add(csp(h.get("content-security-policy")));
        String sniff = h.get("x-content-type-options");
        findings.add(new Finding("X-Content-Type-Options", "nosniff".equalsIgnoreCase(sniff), sniff == null ? "missing" : "nosniff".equalsIgnoreCase(sniff) ? "nosniff" : "should be nosniff"));
        String frame = h.get("x-frame-options");
        boolean frameOk = frame != null && (frame.equalsIgnoreCase("DENY") || frame.equalsIgnoreCase("SAMEORIGIN"));
        findings.add(new Finding("X-Frame-Options", frameOk, frame == null ? "missing" : frameOk ? frame : "should be DENY or SAMEORIGIN"));
        String referrer = h.get("referrer-policy");
        boolean referrerOk = referrer != null && SAFE_REFERRER.contains(referrer.toLowerCase(Locale.ROOT));
        findings.add(new Finding("Referrer-Policy", referrerOk, referrer == null ? "missing" : referrerOk ? referrer : "leaks the full address"));
        return findings;
    }

    public static int score(List<Finding> findings) {
        return (int) findings.stream().filter(Finding::passed).count();
    }

    private static Finding hsts(String value) {
        if (value == null) {
            return new Finding("Strict-Transport-Security", false, "missing");
        }
        for (String part : value.split(";")) {
            String p = part.strip().toLowerCase(Locale.ROOT);
            if (p.startsWith("max-age=")) {
                try {
                    long age = Long.parseLong(p.substring(8));
                    return age >= 31536000 ? new Finding("Strict-Transport-Security", true, "max-age at least a year")
                            : new Finding("Strict-Transport-Security", false, "max-age under a year");
                } catch (NumberFormatException e) {
                    break;
                }
            }
        }
        return new Finding("Strict-Transport-Security", false, "no usable max-age");
    }

    private static Finding csp(String value) {
        if (value == null) {
            return new Finding("Content-Security-Policy", false, "missing");
        }
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.contains("unsafe-inline") || lower.contains("unsafe-eval")) {
            return new Finding("Content-Security-Policy", false, "allows unsafe-inline or unsafe-eval");
        }
        List<String> scripts = sources(lower, "script-src");
        if (scripts == null) {
            scripts = sources(lower, "default-src");
        }
        if (scripts == null) {
            return new Finding("Content-Security-Policy", false, "no default-src or script-src");
        }
        if (scripts.stream().anyMatch(ANYWHERE::contains)) {
            return new Finding("Content-Security-Policy", false, "lets scripts load from anywhere");
        }
        return new Finding("Content-Security-Policy", true, "restricts where scripts load from");
    }

    /** Sources a source list would allow from any site: a policy that names one of these restricts nothing. */
    private static final Set<String> ANYWHERE = Set.of("*", "http:", "https:", "data:", "blob:");

    /** The source list of one directive, or null when the policy does not have it. */
    private static List<String> sources(String policy, String directive) {
        for (String part : policy.split(";")) {
            String[] words = part.strip().split("\\s+");
            if (words.length > 0 && words[0].equals(directive)) {
                return List.of(words).subList(1, words.length);
            }
        }
        return null;
    }
}
