package com.jk.explore.security.tlscheck;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Runs the four-server scan in four numbered acts and prints exact, repeatable results. Each act prints what the scan learned, the report, and how the
 * score and the grade are made, in separate labelled sections.
 */
public class TlsCheckDemo {

    static final String GOOD_SUITE = "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256";
    static final String BROKEN_SUITE = "TLS_RSA_WITH_RC4_128_SHA";

    public static void main(String[] args) {
        System.out.print(run());
    }

    public static String run() {
        StringBuilder out = new StringBuilder();
        Checker checker = new Checker();

        line(out, "ONE. A server that follows the guidance.");
        ServerProfile good = Profiles.good();
        server(out, good);
        report(out, checker.check(good, Profiles.NOW));
        line(out, "  ONE SUITE NAME, PART BY PART (shop.example, legacy.shop.example):");
        SuiteName g = SuiteName.parse(GOOD_SUITE);
        SuiteName b = SuiteName.parse(BROKEN_SUITE);
        line(out, "    " + GOOD_SUITE);
        line(out, "    " + BROKEN_SUITE);
        parts(out, "key exchange", g.keyExchange(), b.keyExchange());
        parts(out, "authentication", g.authentication(), b.authentication());
        parts(out, "cipher", g.cipher(), b.cipher());
        parts(out, "mode", g.mode(), b.mode());
        parts(out, "hash", g.hash(), b.hash());
        parts(out, "forward secret", yes(!CipherSuites.lacksForwardSecrecy(GOOD_SUITE)), yes(!CipherSuites.lacksForwardSecrecy(BROKEN_SUITE)));
        parts(out, "broken", yes(CipherSuites.broken(GOOD_SUITE)), yes(CipherSuites.broken(BROKEN_SUITE)));

        line(out, "TWO. A legacy server.");
        ServerProfile legacy = Profiles.legacy();
        server(out, legacy);
        line(out, "  THE SUITES, READ BY NAME:");
        for (String s : legacy.cipherSuites()) {
            line(out, String.format("    %-38s %s", s, verdict(s)));
        }
        Report before = checker.check(legacy, Profiles.NOW);
        report(out, before);

        line(out, "THREE. A certificate that clients will refuse, and a forgotten HTTP setup.");
        ServerProfile pay = Profiles.brokenCertificate();
        certificateAndHttp(out, pay, good);
        CertFacts c = pay.certificate();
        line(out, "  HOW THE CERTIFICATE CHECKS ARE MADE:");
        line(out, "    expiry    " + day(c.notAfter()) + " -> " + day(Profiles.NOW) + " = "
                + Duration.between(c.notAfter(), Profiles.NOW).toDays() + " day(s) after the end");
        line(out, "    name      " + pay.host() + " in " + c.dnsNames() + "? " + yes(c.dnsNames().stream().anyMatch(n -> Checker.nameMatches(n, pay.host()))));
        line(out, "    wildcard  *.shop.example covers pay.shop.example? " + yes(Checker.nameMatches("*.shop.example", "pay.shop.example")));
        line(out, "    wildcard  *.shop.example covers a.pay.shop.example? " + yes(Checker.nameMatches("*.shop.example", "a.pay.shop.example")));
        report(out, checker.check(pay, Profiles.NOW));
        ServerProfile blog = Profiles.forgotHttp();
        certificateAndHttp(out, blog, good);
        report(out, checker.check(blog, Profiles.NOW));

        line(out, "FOUR. Moving the legacy server to a policy.");
        line(out, "  BEFORE THE POLICY:");
        line(out, "    " + summary(before));
        Policy policy = Policy.INTERMEDIATE;
        line(out, "  THE POLICY " + policy.name() + " (what it sets):");
        line(out, "    protocols    " + protocols(policy.protocols()));
        line(out, "    suites       " + suites(policy.cipherSuites()));
        ServerProfile moved = policy.applyTo(legacy);
        line(out, "    features     " + features(moved));
        Report after = checker.check(moved, Profiles.NOW);
        line(out, "  AFTER THE POLICY:");
        line(out, "    " + summary(after));
        List<String> fixed = before.findings().stream().map(Finding::rule).filter(r -> !after.has(r)).toList();
        line(out, "    fixed by the policy (" + fixed.size() + "):");
        wrap(out, fixed);
        line(out, "    left to fix by hand (" + after.findings().size() + "):");
        wrap(out, after.findings().stream().map(Finding::rule).toList());
        line(out, "  THE GENERATED nginx SETTINGS:");
        for (String l : policy.nginx().lines().toList()) {
            if (l.startsWith("ssl_ciphers ")) {
                line(out, "    ssl_ciphers");
                for (String suite : l.substring("ssl_ciphers ".length(), l.length() - 1).split(":")) {
                    line(out, "      " + suite);
                }
            } else if (l.startsWith("add_header ")) {
                int q = l.indexOf('"');
                line(out, "    " + l.substring(0, q).strip());
                line(out, "      " + l.substring(q));
            } else {
                line(out, "    " + l);
            }
        }
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    /** What a scan learned about one server: the input of the checker. */
    private static void server(StringBuilder out, ServerProfile s) {
        CertFacts c = s.certificate();
        HttpFacts h = s.http();
        line(out, "  THE SERVER " + s.host() + " (what a scan learned):");
        line(out, "    protocols    " + protocols(s.protocols()));
        line(out, "    suites       " + suites(s.cipherSuites()));
        line(out, "    certificate  " + c.keyType() + " " + c.keyBits() + " bits, signed " + c.signature());
        line(out, "    names        " + String.join(", ", c.dnsNames()));
        line(out, "    valid        " + day(c.notBefore()) + " to " + day(c.notAfter()) + " (today " + day(Profiles.NOW) + ")");
        line(out, "    chain        " + (c.chainComplete() ? "complete, ends at a trusted root" : "the intermediate certificate is not sent"));
        line(out, "    HSTS         " + (h.hstsMaxAgeSeconds() < 0 ? "no header"
                : "max-age " + h.hstsMaxAgeSeconds() + " (" + h.hstsMaxAgeSeconds() / 86400 + " days)" + (h.hstsIncludeSubDomains() ? ", subdomains" : "")));
        line(out, "    HTTP         " + (h.httpRedirectsToHttps() ? "redirects to HTTPS" : "no redirect to HTTPS")
                + ", cookie " + (h.sessionCookieSecure() ? "Secure" : "not Secure"));
        line(out, "    features     " + features(s));
    }

    /** Only the certificate and the HTTP settings, when the rest is the same as on the good server. */
    private static void certificateAndHttp(StringBuilder out, ServerProfile s, ServerProfile good) {
        CertFacts c = s.certificate();
        HttpFacts h = s.http();
        line(out, "  THE SERVER " + s.host() + " (what a scan learned):");
        boolean same = s.protocols().equals(good.protocols()) && s.cipherSuites().equals(good.cipherSuites()) && features(s).equals(features(good));
        line(out, "    protocols, suites, features: " + (same ? "the same as " + good.host() : "different"));
        line(out, "    names        " + String.join(", ", c.dnsNames()));
        line(out, "    valid        " + day(c.notBefore()) + " to " + day(c.notAfter()) + " (today " + day(Profiles.NOW) + ")");
        line(out, "    chain        " + (c.chainComplete() ? "complete, ends at a trusted root" : "the intermediate certificate is not sent"));
        line(out, "    HSTS         " + (h.hstsMaxAgeSeconds() < 0 ? "no header" : "max-age " + h.hstsMaxAgeSeconds()));
        line(out, "    HTTP         " + (h.httpRedirectsToHttps() ? "redirects to HTTPS" : "no redirect to HTTPS")
                + ", cookie " + (h.sessionCookieSecure() ? "Secure" : "not Secure"));
    }

    private static String features(ServerProfile s) {
        return (s.serverChoosesCipher() ? "server" : "client") + " picks, compression " + (s.compression() ? "on" : "off") + ", "
                + (s.secureRenegotiation() ? "secure" : "insecure") + " renegotiation";
    }

    /** The findings worst first, with what each costs, and how the score and the grade follow from them. */
    private static void report(StringBuilder out, Report r) {
        line(out, "  THE REPORT FOR " + r.host() + ":");
        if (r.findings().isEmpty()) {
            line(out, "    no findings");
        }
        for (Finding f : r.findings()) {
            line(out, String.format("    %-8s %-22s %4d", f.severity(), f.rule(), -f.severity().penalty()));
        }
        line(out, "  HOW THE GRADE IS MADE:");
        StringBuilder sum = new StringBuilder("100");
        int raw = 100;
        for (Severity s : List.of(Severity.CRITICAL, Severity.HIGH, Severity.MEDIUM, Severity.LOW)) {
            long n = r.count(s);
            if (n > 0) {
                sum.append(" - ").append(n).append(" x ").append(s.penalty());
                raw -= (int) n * s.penalty();
            }
        }
        line(out, "    points  " + sum + " = " + raw);
        line(out, "    score   " + r.score() + (raw < 0 ? " (never below 0)" : ""));
        char byScore = Checker.byScore(r.score());
        String why = r.count(Severity.CRITICAL) > 0 ? "a CRITICAL finding gives F"
                : r.grade() != byScore ? "a HIGH finding caps it at C" : "by score: A 90+, B 75+, C 60+, D 40+";
        line(out, "    grade   " + r.grade() + " (" + why + ")");
    }

    private static void parts(StringBuilder out, String part, String good, String broken) {
        line(out, String.format("    %-16s%-12s%s", part, good, broken));
    }

    private static String verdict(String suite) {
        return CipherSuites.broken(suite) ? "broken"
                : CipherSuites.lacksForwardSecrecy(suite) ? "no forward secrecy" : CipherSuites.cbc(suite) ? "CBC" : "good";
    }

    private static String suites(List<String> all) {
        long fs = all.stream().filter(s -> !CipherSuites.lacksForwardSecrecy(s)).count();
        long broken = all.stream().filter(CipherSuites::broken).count();
        return all.size() + " offered, " + fs + " forward secret, " + broken + " broken";
    }

    private static String protocols(java.util.Set<String> p) {
        return p.stream().sorted().collect(Collectors.joining(" "));
    }

    private static String summary(Report r) {
        return r.host() + "  grade " + r.grade() + " (score " + r.score() + "), " + r.findings().size() + " finding(s)";
    }

    private static String day(Instant i) {
        return i.toString().substring(0, 10);
    }

    private static String yes(boolean b) {
        return b ? "yes" : "no";
    }

    /** Rule ids, a few per line. */
    private static void wrap(StringBuilder out, List<String> rules) {
        List<String> row = new ArrayList<>();
        for (String r : rules) {
            if (String.join(", ", row).length() + r.length() > 56) {
                line(out, "      " + String.join(", ", row) + ",");
                row.clear();
            }
            row.add(r);
        }
        line(out, "      " + String.join(", ", row));
    }
}
