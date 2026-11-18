package com.jk.explore.security.tlsaudit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TlsAuditDemo {

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    static final String HOST = "www.shop.localhost";

    /** The five scans the acts read. */
    record Scans(Report good, Report copied, Report legacy, Report expired, Report wrong, Report byAddress) {
    }

    public static String run() throws Exception {
        Path work = Files.createTempDirectory("tlsaudit");
        LocalCa ca = new LocalCa(work.resolve("ca"));
        ca.issue("good", "-1h", "2160h", false, HOST);
        ca.issue("legacy", "-1h", "2160h", true, HOST);
        ca.issue("expired", "-72h", "-24h", false, HOST);
        ca.issue("wrong", "-1h", "2160h", false, "pay.shop.localhost");
        TestSsl tool = new TestSsl(ca.root(), work.resolve("scans"));
        try (Nginx good = new Nginx(work.resolve("good")); Nginx copied = new Nginx(work.resolve("copied")); Nginx expired = new Nginx(work.resolve("expired")); Nginx wrong = new Nginx(work.resolve("wrong"));
                LegacyServer legacy = new LegacyServer(ca.pkcs12("legacy"))) {
            good.start(Nginx.Settings.guided(), ca.cert("good").toString(), ca.key("good").toString());
            copied.start(Nginx.Settings.copied(), ca.cert("good").toString(), ca.key("good").toString());
            expired.start(Nginx.Settings.guided(), ca.cert("expired").toString(), ca.key("expired").toString());
            wrong.start(Nginx.Settings.guided(), ca.cert("wrong").toString(), ca.key("wrong").toString());
            CompletableFuture<Report> goodScan = CompletableFuture.supplyAsync(() -> scan(tool, good.port()));
            CompletableFuture<Report> copiedScan = CompletableFuture.supplyAsync(() -> scan(tool, copied.port()));
            CompletableFuture<Report> legacyScan = CompletableFuture.supplyAsync(() -> scan(tool, legacy.port()));
            CompletableFuture<Report> expiredScan = CompletableFuture.supplyAsync(() -> scan(tool, expired.port()));
            CompletableFuture<Report> wrongScan = CompletableFuture.supplyAsync(() -> scan(tool, wrong.port()));
            CompletableFuture<Report> addressScan = CompletableFuture.supplyAsync(() -> {
                try {
                    return tool.scanByAddress(good.port());
                } catch (IOException | InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            });
            return print(new Scans(goodScan.join(), copiedScan.join(), legacyScan.join(), expiredScan.join(), wrongScan.join(), addressScan.join()));
        }
    }

    /** Everything the demo prints, from the six reports alone, so a test can feed it fixed reports. */
    static String print(Scans s) {
        StringBuilder out = new StringBuilder();
        act1(out, s.good());
        act2(out, s.copied(), s.good());
        act3(out, s);
        act4(out, s.good(), s.expired(), s.wrong());
        act5(out, s.good(), s.byAddress());
        return out.toString();
    }

    private static Report scan(TestSsl tool, int port) {
        try {
            return tool.scan(HOST, port);
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    /** A testssl finding, shortened to what the act needs; a version number is never printed, it changes with every update. */
    static String finding(Report r, String id) {
        String text = r.text(id);
        if (id.equals("banner_server")) {
            text = text.replaceAll("/[0-9.]+", "/<version>");
        }
        return text.replace(" (same w/o SNI)", "").replaceAll(" \\(=.*", "").replaceAll(" \\+ .*", "");
    }

    /** Rows of id, severity and finding, as testssl.sh wrote them. */
    private static void rows(StringBuilder out, Report r, String... ids) {
        int width = 0;
        int severity = 0;
        for (String id : ids) {
            width = Math.max(width, id.length());
            severity = Math.max(severity, r.severity(id).length());
        }
        for (String id : ids) {
            line(out, "  " + pad(id, width + 2) + pad(r.severity(id), severity + 2) + finding(r, id));
        }
    }

    private static String pad(String s, int width) {
        return String.format("%-" + width + "s", s);
    }

    static String protocols(Report r) {
        List<String> out = new ArrayList<>();
        for (String id : new String[] {"SSLv2", "SSLv3", "TLS1", "TLS1_1", "TLS1_2", "TLS1_3"}) {
            if (r.offered(id)) {
                out.add(id.replace("TLS1_", "TLS 1.").replace("TLS1", "TLS 1.0").replace("SSLv", "SSL "));
            }
        }
        return String.join(", ", out);
    }

    private static String gate(Report r) {
        List<String> failures = Policy.failures(r);
        return failures.isEmpty() ? "pass" : "fail, " + failures.size() + " rule" + (failures.size() == 1 ? "" : "s");
    }

    /** A list printed over as many lines as it needs, each under 60 characters. */
    private static void wrapped(StringBuilder out, String key, List<String> items) {
        StringBuilder row = new StringBuilder("  " + pad(key, 10));
        for (int i = 0; i < items.size(); i++) {
            String item = items.get(i) + (i < items.size() - 1 ? "," : "");
            if (row.length() + item.length() > 60) {
                line(out, row.toString().stripTrailing());
                row = new StringBuilder("  " + pad("", 10));
            }
            row.append(item).append(' ');
        }
        line(out, row.toString().stripTrailing());
    }

    static void act1(StringBuilder out, Report good) {
        Nginx.Settings guided = Nginx.Settings.guided();
        line(out, "ONE. A server that follows the guidance.");
        line(out, "THE SERVER (nginx, the guided lines):");
        line(out, "  ssl_protocols  " + guided.protocols());
        line(out, "  ssl_ciphers    " + Nginx.CIPHERS.split(":").length + " suites, every one ECDHE");
        line(out, "  add_header     HSTS and " + Nginx.HEADERS.size() + " security headers");
        line(out, "  server_tokens  off");
        line(out, "  certificate    " + HOST + ", from the lab's root");
        line(out, "THE SCAN (one run of testssl.sh per server):");
        line(out, "  testssl.sh " + String.join(" ", TestSsl.CHECKS) + " --jsonfile scan.json");
        line(out, "      --ip 127.0.0.1 " + HOST + ":<port>");
        line(out, "  -p protocols, -s cipher lists, -S certificate, -h headers");
        line(out, "THE REPORT (id, severity, finding, as testssl.sh wrote them):");
        rows(out, good, "TLS1", "TLS1_1", "TLS1_3", "cipherlist_STRONG_FS", "cipherlist_OBSOLETED", "cert_chain_of_trust", "cert_trust", "cert_expirationStatus",
                "HSTS_time", "banner_server", "cert_revocation");
        line(out, "THE GATE, RULE BY RULE (the shop's " + Policy.RULES.size() + " rules over that report):");
        for (Policy.Rule rule : Policy.RULES) {
            line(out, "  " + pad(rule.id(), 17) + pad(rule.reads(), 23) + (rule.holds().test(good) ? "holds" : "broken"));
        }
        Policy.Exception exception = Policy.EXCEPTIONS.get(0);
        line(out, "THE EXCEPTION (a decision written down, with its reason):");
        line(out, "  rule    " + exception.ruleId());
        line(out, "  reason  " + exception.reason());
        line(out, "THE RESULT:");
        line(out, "  testssl HIGH or CRITICAL  " + good.idsAt("HIGH", "CRITICAL"));
        line(out, "  rules broken              " + Policy.broken(good));
        line(out, "  of them excepted          " + Policy.broken(good).stream().filter(id -> !Policy.failures(good).contains(id)).toList());
        line(out, "  the gate                  " + Policy.verdict(good));
    }

    static void act2(StringBuilder out, Report copied, Report good) {
        line(out, "TWO. A configuration copied from the first search result.");
        line(out, "THE COPIED LINES (same certificate, same ssl_protocols):");
        line(out, "  ssl_ciphers    (none: nginx's default list)");
        line(out, "  add_header     (none: no HSTS, no security headers)");
        line(out, "  server_tokens  on (the default)");
        line(out, "THE REPORT (the ids that changed):");
        rows(out, copied, "cipherlist_OBSOLETED", "HSTS", "security_headers", "banner_server");
        WorkedOut.Hsts hsts = WorkedOut.hsts(good.text("HSTS_time"));
        line(out, "THE HSTS TIME, WORKED OUT (the guided server's header):");
        line(out, "  max-age   " + hsts.seconds() + " seconds");
        line(out, "  in days   " + hsts.seconds() + " / " + WorkedOut.DAY + " = " + hsts.days() + " days");
        line(out, "  minimum   " + hsts.minimum() + " seconds = " + hsts.minimumDays() + " days (testssl's)");
        line(out, "  result    " + hsts.days() + (hsts.ok() ? " >= " : " < ") + hsts.minimumDays() + ", so " + good.severity("HSTS_time"));
        line(out, "  copied    no header at all, so " + copied.severity("HSTS"));
        line(out, "THE RESULT:");
        line(out, "  testssl HIGH or CRITICAL  " + copied.idsAt("HIGH", "CRITICAL"));
        wrapped(out, "broken", Policy.failures(copied));
        line(out, "  the gate  " + Policy.verdict(copied).replaceAll(" \\[.*", "") + "; the guided lines: " + Policy.verdict(good));
        line(out, "  a browser loads the page: none of this shows");
    }

    static void act3(StringBuilder out, Scans s) {
        Report legacy = s.legacy();
        line(out, "THREE. A server that still speaks what old clients speak.");
        line(out, "THE SERVER (written with the JDK, told to allow it):");
        line(out, "  protocols    " + String.join(" ", LegacyServer.PROTOCOLS));
        line(out, "  suites       TLS_RSA_WITH_* only: RSA key exchange");
        line(out, "  certificate  " + legacy.text("cert_keySize").replaceAll(" \\(.*", ""));
        line(out, "THE REPORT (the ids that changed):");
        rows(out, legacy, "TLS1", "TLS1_1", "TLS1_3", "cipherlist_STRONG_NOFS", "cipherlist_STRONG_FS", "cipherlist_OBSOLETED", "HSTS", "security_headers");
        line(out, "THE RATING AND THE RULE (one finding, two readers):");
        for (String id : List.of("TLS1", "TLS1_1")) {
            line(out, "  " + pad(id, 8) + "testssl: " + pad(legacy.severity(id), 6) + "the rule protocols: " + (Policy.broken(legacy).contains("protocols") ? "broken" : "holds"));
        }
        line(out, "A GATE ON testssl's SEVERITY (fail on HIGH or CRITICAL):");
        line(out, "  guided  " + s.good().idsAt("HIGH", "CRITICAL"));
        line(out, "  copied  " + s.copied().idsAt("HIGH", "CRITICAL"));
        line(out, "  legacy  " + legacy.idsAt("HIGH", "CRITICAL"));
        line(out, "  the three look the same to it");
        line(out, "THE SHOP'S GATE (its own rules):");
        line(out, "  guided  " + gate(s.good()));
        line(out, "  copied  " + gate(s.copied()));
        line(out, "  legacy  " + gate(legacy));
        wrapped(out, "broken", Policy.failures(legacy));
    }

    static void act4(StringBuilder out, Report good, Report expired, Report wrong) {
        line(out, "FOUR. Certificates that clients will refuse.");
        line(out, "THE REPORT FOR THE EXPIRED CERTIFICATE:");
        rows(out, expired, "cert_chain_of_trust", "cert_expirationStatus");
        line(out, "THE EXPIRY, WORKED OUT (not after - not before):");
        line(out, "  good     " + WorkedOut.lifetimeHours(good.text("cert_notBefore"), good.text("cert_notAfter")) + " hours = "
                + WorkedOut.lifetimeHours(good.text("cert_notBefore"), good.text("cert_notAfter")) / 24 + " days");
        line(out, "           days left " + good.text("cert_expirationStatus") + ", so " + good.severity("cert_expirationStatus"));
        line(out, "  expired  " + WorkedOut.lifetimeHours(expired.text("cert_notBefore"), expired.text("cert_notAfter")) + " hours, and it ended before the scan");
        line(out, "           so " + expired.severity("cert_expirationStatus") + ", and the chain fails with it");
        line(out, "THE REPORT FOR THE CERTIFICATE OF ANOTHER NAME:");
        rows(out, wrong, "cert_trust");
        line(out, "THE NAME, WORKED OUT (is the name asked for listed?):");
        line(out, "  asked for     " + HOST);
        line(out, "  certificate   [" + wrong.text("cert_subjectAltName") + "]");
        line(out, "  listed        " + (WorkedOut.listed(HOST, wrong.text("cert_subjectAltName")) ? "yes" : "no") + ", so " + wrong.severity("cert_trust"));
        line(out, "  *.shop.localhost would list it: "
                + (WorkedOut.listed(HOST, "*.shop.localhost") ? "yes" : "no") + " (one label)");
        line(out, "THE RESULT:");
        line(out, "  expired         " + Policy.verdict(expired));
        line(out, "  another name    " + Policy.verdict(wrong));
        line(out, "  every browser refuses both, whatever else is right");
    }

    static void act5(StringBuilder out, Report good, Report byAddress) {
        line(out, "FIVE. The bill.");
        line(out, "THE SCAN BY ADDRESS (the same guided server):");
        line(out, "  testssl.sh " + String.join(" ", TestSsl.CHECKS) + " --jsonfile scan.json");
        line(out, "      127.0.0.1:<port>");
        line(out, "THE REPORT:");
        rows(out, byAddress, "cert_trust");
        line(out, "THE NAME, WORKED OUT:");
        line(out, "  asked for     127.0.0.1");
        line(out, "  certificate   [" + byAddress.text("cert_subjectAltName") + "]");
        line(out, "  listed        " + (WorkedOut.listed("127.0.0.1", byAddress.text("cert_subjectAltName")) ? "yes" : "no") + ", so " + byAddress.severity("cert_trust"));
        line(out, "THE RESULT:");
        line(out, "  by name     " + Policy.verdict(good));
        line(out, "  by address  " + Policy.verdict(byAddress) + "  <- A FALSE ALARM");
        line(out, "WHAT A SCAN DOES NOT SEE:");
        line(out, "  the application, the other paths, tomorrow's change");
        line(out, "  so: scan by name, rerun after every change and on a schedule");
    }
}
