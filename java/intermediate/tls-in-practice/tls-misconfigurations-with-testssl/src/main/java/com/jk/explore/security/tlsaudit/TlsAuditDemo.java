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

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
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
            act1(out, goodScan.join());
            act2(out, copiedScan.join(), goodScan.join());
            act3(out, legacyScan.join());
            act4(out, expiredScan.join(), wrongScan.join());
            act5(out, goodScan.join(), legacyScan.join(), addressScan.join());
        }
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

    private static String protocols(Report r) {
        List<String> out = new ArrayList<>();
        for (String id : new String[] {"SSLv2", "SSLv3", "TLS1", "TLS1_1", "TLS1_2", "TLS1_3"}) {
            if (r.offered(id)) {
                out.add(id.replace("TLS1_", "TLS 1.").replace("TLS1", "TLS 1.0").replace("SSLv", "SSL "));
            }
        }
        return String.join(", ", out);
    }

    private static void act1(StringBuilder out, Report good) {
        line(out, "ONE. A server that follows the guidance, scanned by testssl.sh.");
        line(out, "  the scan reads the protocols, the cipher suites, the certificate and the headers of one server in about fifteen seconds");
        line(out, "  protocols offered: " + protocols(good));
        line(out, "  the certificate: chain " + good.text("cert_chain_of_trust").replace(".", "") + ", validity " + good.severity("cert_expirationStatus") + ", forward secrecy with strong suites: " + good.text("cipherlist_STRONG_FS") + ", HSTS " + good.text("HSTS_time").replaceAll(" \\(.*", ""));
        line(out, "  testssl rates by itself: the findings it calls HIGH or CRITICAL: " + good.idsAt("HIGH", "CRITICAL"));
        line(out, "  the shop's own policy has " + Policy.RULES.size() + " rules; the ones this server breaks: " + Policy.broken(good) + ", of which excepted: " + Policy.EXCEPTIONS.stream().map(Policy.Exception::ruleId).toList() + " (" + Policy.EXCEPTIONS.get(0).reason() + ")");
        line(out, "  the gate: " + Policy.verdict(good));
    }

    private static void act2(StringBuilder out, Report copied, Report good) {
        line(out, "TWO. A configuration copied from the first search result.");
        line(out, "  the same certificate and the same protocols, without ssl_ciphers, headers or server_tokens off");
        line(out, "  the rules it breaks: " + Policy.failures(copied));
        line(out, "  what testssl says about them: HSTS " + copied.severity("HSTS") + " (" + copied.text("HSTS") + "), security headers " + copied.severity("security_headers") + ", obsolete cipher suites " + copied.severity("cipherlist_OBSOLETED") + " (" + copied.text("cipherlist_OBSOLETED") + "), server banner shows a version: " + !copied.text("banner_server").replaceAll("[^0-9]", "").isEmpty());
        line(out, "  nothing here stops a browser from loading the page: every one of these is invisible to the person using it");
        line(out, "  the guided configuration, with the same certificate: " + Policy.verdict(good));
    }

    private static void act3(StringBuilder out, Report legacy) {
        line(out, "THREE. A server that still speaks what old clients speak.");
        line(out, "  written with the JDK, which has to be told to do it: protocols offered: " + protocols(legacy));
        line(out, "  forward secrecy with strong suites: " + legacy.text("cipherlist_STRONG_FS") + " (" + legacy.severity("cipherlist_STRONG_FS") + "); the key exchange is RSA only, so a stolen key opens every recorded session");
        line(out, "  testssl rates TLS 1.0 and 1.1 as " + legacy.severity("TLS1") + " and " + legacy.severity("TLS1_1") + ", and the shop's policy refuses them: the rating is the tool's, the rule is yours");
        line(out, "  the rules it breaks: " + Policy.failures(legacy));
    }

    private static void act4(StringBuilder out, Report expired, Report wrong) {
        line(out, "FOUR. Certificates that clients will refuse.");
        line(out, "  an expired certificate: chain " + expired.severity("cert_chain_of_trust") + " (" + expired.text("cert_chain_of_trust").replace(".", "") + "), expiry " + expired.severity("cert_expirationStatus") + " (" + expired.text("cert_expirationStatus") + ")");
        line(out, "  the rules it breaks: " + Policy.failures(expired).stream().filter(id -> id.equals("chain") || id.equals("expiry") || id.equals("name")).toList());
        line(out, "  a certificate for pay.shop.localhost on www.shop.localhost: " + wrong.severity("cert_trust") + " (" + wrong.text("cert_trust").replaceAll(" \\(.*", "") + ")");
        line(out, "  the rules it breaks: " + Policy.failures(wrong).stream().filter(id -> id.equals("chain") || id.equals("expiry") || id.equals("name")).toList());
        line(out, "  both of these stop every browser, and nothing else about the server matters until they are fixed");
    }

    private static void act5(StringBuilder out, Report good, Report legacy, Report byAddress) {
        line(out, "FIVE. The bill.");
        line(out, "  the same good server, scanned by its address instead of its name: " + Policy.verdict(byAddress) + "  <- A FALSE ALARM: THE CERTIFICATE IS NOT FOR AN ADDRESS");
        line(out, "  an exception is a decision written down with its reason, and a gate that has none will be switched off the first time it is wrong");
        line(out, "  a scan is a snapshot of one moment, of the TLS handshake and of the page at /; it does not see the application, the other paths or a server that changes tomorrow");
        line(out, "  testssl reports dozens of checks per server; deciding which of them the shop requires is the work, and the policy is a file under version control");
        line(out, "  rerun the scan after every change of certificate, of server or of its configuration, and on a schedule, because a certificate runs out without anyone touching it");
    }
}
