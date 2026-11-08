package com.jk.explore.security.httpsnginx;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Runs the online store story in five numbered acts against real nginx containers and prints repeatable results. */
public class HttpsNginxDemo {

    /** The shop's name, as the client asks for it and the certificate names it. */
    static final String HOST = TlsClient.Options.trusting().host();

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text. It needs a running Docker, so its test is tagged "docker". */
    public static String run() throws IOException, InterruptedException {
        StringBuilder out = new StringBuilder();
        act1Terminate(out);
        act2Protocols(out);
        act3Redirect(out);
        act4Trust(out);
        act5ProxyAndChecks(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    static int score(Map<String, String> headers) {
        return HeaderAudit.score(HeaderAudit.audit(headers));
    }

    /** One point per header, added up in the open. */
    static String points(Map<String, String> headers) {
        List<HeaderAudit.Finding> findings = HeaderAudit.audit(headers);
        return findings.stream().map(f -> f.passed() ? "1" : "0").collect(Collectors.joining(" + "))
                + " = " + HeaderAudit.score(findings) + " of 5";
    }

    private static String refusedOrNot(boolean refused) {
        return refused ? "refused" : "accepted";
    }

    /** The lines of a configuration that start with one of the directives, as nginx will read them. */
    private static List<String> directives(NginxConfig.Variant variant, String... names) {
        return NginxConfig.text(variant).lines().map(String::strip)
                .filter(l -> java.util.Arrays.stream(names).anyMatch(n -> l.startsWith(n + " ")))
                .distinct().toList();
    }

    private static void act1Terminate(StringBuilder out) throws IOException, InterruptedException {
        line(out, "ONE. nginx ends the TLS for the shop.");
        line(out, "  THE CONFIGURATION (the HTTPS server block, in short):");
        for (String d : directives(NginxConfig.Variant.CAREFUL, "listen 443", "http2", "ssl_certificate", "ssl_protocols", "proxy_pass")) {
            line(out, "    " + d);
        }
        line(out, "  THE REQUEST (a Java client that trusts the demo root):");
        line(out, "    GET https://" + HOST + "/");
        try (Nginx nginx = Nginx.start(NginxConfig.Variant.CAREFUL)) {
            TlsClient.Reply reply = TlsClient.get(nginx.httpsPort(), "/", TlsClient.Options.trusting());
            line(out, "  THE RESPONSE:");
            line(out, "    " + reply.status() + " over " + reply.protocol()
                    + (reply.version() == java.net.http.HttpClient.Version.HTTP_2 ? ", HTTP/2 agreed in the handshake" : ", HTTP/1.1"));
            line(out, "    security headers " + points(reply.headers()) + ", Server: " + reply.headers().get("server"));
            line(out, "    the app behind nginx: " + reply.body().strip());
        }
    }

    private static void act2Protocols(StringBuilder out) throws IOException, InterruptedException {
        line(out, "TWO. Which TLS versions the shop allows.");
        line(out, "  THE CONFIGURATIONS:");
        line(out, "    strict   " + directives(NginxConfig.Variant.CAREFUL, "ssl_protocols").get(0));
        line(out, "    compat   " + directives(NginxConfig.Variant.COMPAT, "ssl_protocols").get(0));
        String ciphers = directives(NginxConfig.Variant.COMPAT, "ssl_ciphers").get(0).replace("ssl_ciphers ", "").replace(";", "");
        line(out, "             ssl_ciphers, three of them:");
        for (String cipher : ciphers.split(":")) {
            line(out, "               " + cipher);
        }
        String[][] runs = new String[4][];
        try (Nginx nginx = Nginx.start(NginxConfig.Variant.CAREFUL)) {
            runs[0] = new String[] {"strict", "TLS 1.2", refusedOrNot(TlsClient.refused(nginx.httpsPort(), "/", TlsClient.Options.trusting().withProtocols("TLSv1.2")))};
            runs[1] = new String[] {"strict", "TLS 1.3", TlsClient.get(nginx.httpsPort(), "/", TlsClient.Options.trusting().withProtocols("TLSv1.3")).protocol()};
        }
        try (Nginx nginx = Nginx.start(NginxConfig.Variant.COMPAT)) {
            runs[2] = new String[] {"compat", "TLS 1.2", TlsClient.get(nginx.httpsPort(), "/", TlsClient.Options.trusting().withProtocols("TLSv1.2")).protocol()};
            runs[3] = new String[] {"compat", "TLS 1.3", TlsClient.get(nginx.httpsPort(), "/", TlsClient.Options.trusting().withProtocols("TLSv1.3")).protocol()};
        }
        line(out, "  THE RESULTS (server, the only version the client offers):");
        for (String[] run : runs) {
            line(out, "    " + run[0] + "   " + run[1] + " client   " + run[2]);
        }
        line(out, "  allow TLS 1.2 only while customers still need it,");
        line(out, "  and keep the list of ciphers short");
    }

    private static void act3Redirect(StringBuilder out) throws IOException, InterruptedException {
        line(out, "THREE. The plain port, the redirect and HSTS.");
        line(out, "  THE REQUEST (to the plain port):");
        line(out, "    GET http://" + HOST + "/orders?id=7");
        try (Nginx nginx = Nginx.start(NginxConfig.Variant.CAREFUL)) {
            Probe.Reply plain = Probe.get(nginx.httpPort(), "/orders?id=7");
            line(out, "  THE RESPONSE:");
            line(out, "    " + plain.status() + "  Location: " + plain.headers().get("location"));
            line(out, "    Strict-Transport-Security: "
                    + (plain.headers().containsKey("strict-transport-security") ? "sent" : "none (ignored over HTTP)"));
            TlsClient.Reply secure = TlsClient.get(nginx.httpsPort(), "/", TlsClient.Options.trusting());
            String hsts = secure.headers().get("strict-transport-security");
            long seconds = Long.parseLong(hsts.replaceAll(".*max-age=(\\d+).*", "$1"));
            line(out, "  THE HTTPS ANSWER:");
            line(out, "    Strict-Transport-Security:");
            line(out, "      " + hsts);
            line(out, "    " + seconds + " / 86400 seconds a day = " + seconds / 86_400 + " days");
        }
        line(out, "  the very first visit is still plain: HSTS protects every");
        line(out, "  visit after it, and preloading protects the first");
    }

    private static void act4Trust(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FOUR. Who the client trusts, and who the server trusts.");
        line(out, "  THE ATTEMPTS (the client checks the server):");
        line(out, "    a  the JDK's own trusted roots, " + HOST);
        line(out, "    b  the demo root trusted, " + HOST);
        line(out, "    c  the demo root trusted, by the address 127.0.0.1");
        line(out, "    d  an impostor: the right name, signed by nobody trusted");
        String a;
        String b;
        String c;
        String d;
        try (Nginx nginx = Nginx.start(NginxConfig.Variant.CAREFUL)) {
            int port = nginx.httpsPort();
            a = refusedOrNot(TlsClient.refused(port, "/", TlsClient.Options.trusting().withDefaultTrust()));
            b = refusedOrNot(TlsClient.refused(port, "/", TlsClient.Options.trusting()));
            c = refusedOrNot(TlsClient.refused(port, "/", TlsClient.Options.trusting().withHost("127.0.0.1")));
        }
        try (Nginx nginx = Nginx.start(NginxConfig.Variant.IMPOSTOR)) {
            d = refusedOrNot(TlsClient.refused(nginx.httpsPort(), "/", TlsClient.Options.trusting()));
        }
        line(out, "  THE RESULTS:");
        line(out, "    a  " + a + "   the demo root is not in the JDK's list");
        line(out, "    b  " + b);
        line(out, "    c  " + c + "   the certificate names " + HOST);
        line(out, "    d  " + d + "   no trusted signer");
        line(out, "  THE CLIENTS (" + directives(NginxConfig.Variant.MTLS, "ssl_verify_client").get(0).replace(";", "") + "):");
        line(out, "    e  no certificate");
        line(out, "    f  Ana's certificate");
        line(out, "    g  Ana's certificate, and X-Client-DN: CN=Admin");
        try (Nginx nginx = Nginx.start(NginxConfig.Variant.MTLS)) {
            int port = nginx.httpsPort();
            String e = refusedOrNot(TlsClient.refused(port, "/", TlsClient.Options.trusting()));
            TlsClient.Reply ana = TlsClient.get(port, "/", TlsClient.Options.trusting().withClientCertificate());
            TlsClient.Reply forged = TlsClient.get(port, "/", TlsClient.Options.trusting().withClientCertificate().withHeader("X-Client-DN", "CN=Admin"));
            line(out, "  THE RESULTS (what the app behind nginx was told):");
            line(out, "    e  " + e);
            line(out, "    f  " + ana.status() + "  " + ana.body().strip().replace("app saw ", ""));
            line(out, "    g  " + forged.status() + "  " + forged.body().strip().replace("app saw ", ""));
        }
        line(out, "  nginx sets X-Client-DN itself and drops Ana's");
    }

    private static void act5ProxyAndChecks(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FIVE. The proxy trap, a config check and a score.");
        String forged = "GET /app HTTP/1.1\r\nHost: shop.example.test\r\nX-Forwarded-Proto: https\r\nConnection: close\r\n\r\n";
        line(out, "  THE REQUEST (plain HTTP, with a forged header):");
        line(out, "    GET /app   X-Forwarded-Proto: https");
        line(out, "  THE RESPONSES (what the app behind nginx was told):");
        try (Nginx nginx = Nginx.start(NginxConfig.Variant.PROXY_TRAP)) {
            line(out, "    nginx copies it   " + Probe.send(nginx.httpPort(), forged).body().strip().replace("app saw ", "") + "  <- FOOLED");
        }
        try (Nginx nginx = Nginx.start(NginxConfig.Variant.PROXY_SAFE)) {
            line(out, "    nginx sets it     " + Probe.send(nginx.httpPort(), forged).body().strip().replace("app saw ", ""));
        }
        String good = Nginx.configTest(NginxConfig.text(NginxConfig.Variant.CAREFUL));
        String broken = Nginx.configTest(NginxConfig.text(NginxConfig.Variant.CAREFUL, "missing"));
        line(out, "  THE CONFIG CHECKS (nginx -t, before any customer):");
        line(out, "    the careful configuration     "
                + (good.contains("syntax is ok") && good.contains("exit code 0") ? "syntax ok" : "failed"));
        line(out, "    the certificate file missing  " + (broken.contains("exit code 1") ? "refused" : "not caught"));
        List<ConfigScore.Finding> careful = ConfigScore.score(NginxConfig.text(NginxConfig.Variant.CAREFUL));
        List<ConfigScore.Finding> copied = ConfigScore.score(SLOPPY);
        line(out, "  THE SCORE (seven TLS habits):");
        line(out, "    " + String.format("%-26s", "") + "careful  copied");
        for (int i = 0; i < careful.size(); i++) {
            line(out, "    " + String.format("%-26s", careful.get(i).check()) + String.format("%-9s", yes(careful.get(i))) + yes(copied.get(i)));
        }
        line(out, "    careful   " + sum(careful));
        line(out, "    copied    " + sum(copied));
        line(out, "  OCSP stapling needs a public authority: described, not run");
    }

    private static String yes(ConfigScore.Finding finding) {
        return finding.passed() ? "yes" : "no";
    }

    private static String sum(List<ConfigScore.Finding> findings) {
        return findings.stream().map(f -> f.passed() ? "1" : "0").collect(Collectors.joining(" + "))
                + " = " + ConfigScore.points(findings) + " of 7";
    }

    /** What many copied configurations look like: old protocols, HSTS without always, no redirect, and the client's word about the protocol. */
    static final String SLOPPY = String.join("\n", List.of(
            "http {",
            "  server_tokens off;",
            "  server {",
            "    listen 80;",
            "    listen 443 ssl;",
            "    ssl_protocols TLSv1 TLSv1.1 TLSv1.2;",
            "    add_header Strict-Transport-Security \"max-age=300\";",
            "    location / {",
            "      proxy_pass http://app:8080;",
            "      proxy_set_header X-Forwarded-Proto $http_x_forwarded_proto;",
            "    }",
            "  }",
            "}", ""));
}
