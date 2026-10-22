package com.jk.explore.security.tlsaudit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** The shop's own rules for a TLS server, written over what testssl.sh reports, and the exceptions that have a reason and a name. */
final class Policy {

    record Rule(String id, String text, Predicate<Report> holds) {
    }

    /** A rule that is not enforced, with the reason. */
    record Exception(String ruleId, String reason) {
    }

    static final List<Rule> RULES = List.of(
            new Rule("protocols", "no protocol older than TLS 1.2", r -> !r.offered("SSLv2") && !r.offered("SSLv3") && !r.offered("TLS1") && !r.offered("TLS1_1")),
            new Rule("tls13", "TLS 1.3 is offered", r -> r.offered("TLS1_3")),
            new Rule("forward-secrecy", "forward secrecy with a strong suite", r -> r.severity("cipherlist_STRONG_FS").equals("OK")),
            new Rule("obsolete-suites", "no obsolete cipher suites", r -> !r.text("cipherlist_OBSOLETED").startsWith("offered")),
            new Rule("chain", "the certificate chains to a trusted root", r -> r.severity("cert_chain_of_trust").equals("OK")),
            new Rule("name", "the certificate is for the name asked for", r -> !r.severity("cert_trust").equals("HIGH") && !r.severity("cert_trust").equals("CRITICAL")),
            new Rule("expiry", "the certificate is valid and not close to its end", r -> r.severity("cert_expirationStatus").equals("OK")),
            new Rule("hsts", "Strict-Transport-Security is sent", r -> r.severity("HSTS_time").equals("OK")),
            new Rule("headers", "the security headers are sent", r -> !r.severity("security_headers").equals("MEDIUM")),
            new Rule("banner", "the server banner hides the version", r -> r.text("banner_server").replaceAll("[^0-9]", "").isEmpty()),
            new Rule("revocation", "the certificate names a CRL or an OCSP responder", r -> !r.severity("cert_revocation").equals("HIGH")));

    /** The lab's authority publishes neither a list nor a responder, and the step-ca project shows why: the certificates are one day or three months long. */
    static final List<Exception> EXCEPTIONS = List.of(new Exception("revocation", "the lab authority publishes no CRL or OCSP responder"));

    private Policy() {
    }

    /** The rules that do not hold and have no exception, in order. */
    static List<String> failures(Report report) {
        List<String> out = new ArrayList<>();
        for (Rule rule : RULES) {
            if (!rule.holds().test(report) && EXCEPTIONS.stream().noneMatch(e -> e.ruleId().equals(rule.id()))) {
                out.add(rule.id());
            }
        }
        return out;
    }

    /** Every rule that does not hold, with or without an exception. */
    static List<String> broken(Report report) {
        return RULES.stream().filter(r -> !r.holds().test(report)).map(Rule::id).toList();
    }

    static String verdict(Report report) {
        List<String> failures = failures(report);
        return failures.isEmpty() ? "pass" : "fail " + failures;
    }

    static Map<String, String> text() {
        Map<String, String> out = new java.util.LinkedHashMap<>();
        RULES.forEach(r -> out.put(r.id(), r.text()));
        return out;
    }
}
