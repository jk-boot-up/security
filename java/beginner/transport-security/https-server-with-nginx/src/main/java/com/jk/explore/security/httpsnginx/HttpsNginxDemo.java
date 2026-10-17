package com.jk.explore.security.httpsnginx;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/** Runs the online store story in five numbered acts against real nginx containers and prints repeatable results. */
public class HttpsNginxDemo {

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

    private static String shown(boolean flag) {
        return flag ? "yes" : "no";
    }

    private static String refusedOrNot(boolean refused) {
        return refused ? "refused" : "accepted";
    }

    private static void act1Terminate(StringBuilder out) throws IOException, InterruptedException {
        line(out, "ONE. nginx ends the TLS for the shop.");
        try (Nginx nginx = Nginx.start(NginxConfig.Variant.CAREFUL)) {
            TlsClient.Reply reply = TlsClient.get(nginx.httpsPort(), "/", TlsClient.Options.trusting());
            line(out, "  a Java client that trusts the demo root asks https://shop.example.test/ and gets " + reply.status() + " over " + reply.protocol());
            line(out, "  HTTP/2 was agreed during the handshake: " + shown(reply.version() == java.net.http.HttpClient.Version.HTTP_2));
            line(out, "  the five security headers score " + score(reply.headers()) + " of 5, and the Server header says just " + reply.headers().get("server"));
            line(out, "  behind nginx the application, on a plain local port, answered: " + reply.body().strip());
        }
    }

    private static void act2Protocols(StringBuilder out) throws IOException, InterruptedException {
        line(out, "TWO. Which TLS versions the shop allows.");
        try (Nginx nginx = Nginx.start(NginxConfig.Variant.CAREFUL)) {
            line(out, "  nginx set to TLS 1.3 only: a client limited to TLS 1.2 is "
                    + refusedOrNot(TlsClient.refused(nginx.httpsPort(), "/", TlsClient.Options.trusting().withProtocols("TLSv1.2"))));
            line(out, "  the same server and a TLS 1.3 client: " + TlsClient.get(nginx.httpsPort(), "/", TlsClient.Options.trusting().withProtocols("TLSv1.3")).protocol());
        }
        try (Nginx nginx = Nginx.start(NginxConfig.Variant.COMPAT)) {
            line(out, "  nginx set to allow TLS 1.2 as well, with three strong ciphers: the TLS 1.2 client gets "
                    + TlsClient.get(nginx.httpsPort(), "/", TlsClient.Options.trusting().withProtocols("TLSv1.2")).protocol());
            line(out, "  a TLS 1.3 client on the same server still gets " + TlsClient.get(nginx.httpsPort(), "/", TlsClient.Options.trusting().withProtocols("TLSv1.3")).protocol());
        }
        line(out, "  allow TLS 1.2 only while customers still need it, and keep the list of ciphers short");
    }

    private static void act3Redirect(StringBuilder out) throws IOException, InterruptedException {
        line(out, "THREE. The plain port, the redirect and HSTS.");
        try (Nginx nginx = Nginx.start(NginxConfig.Variant.CAREFUL)) {
            Probe.Reply plain = Probe.get(nginx.httpPort(), "/orders?id=7");
            line(out, "  GET http://shop.example.test/orders?id=7 answers " + plain.status() + " and Location " + plain.headers().get("location"));
            line(out, "  the plain answer carries HSTS: " + shown(plain.headers().containsKey("strict-transport-security")) + " (a browser ignores HSTS sent over plain HTTP)");
            TlsClient.Reply secure = TlsClient.get(nginx.httpsPort(), "/", TlsClient.Options.trusting());
            line(out, "  the HTTPS answer carries HSTS: " + secure.headers().get("strict-transport-security"));
            line(out, "  the very first visit is still plain, so HSTS protects every visit after it, and preloading protects the first");
        }
    }

    private static void act4Trust(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FOUR. Who the client trusts, and who the server trusts.");
        try (Nginx nginx = Nginx.start(NginxConfig.Variant.CAREFUL)) {
            int port = nginx.httpsPort();
            line(out, "  the JDK's own list of trusted roots does not know the demo root: " + refusedOrNot(TlsClient.refused(port, "/", TlsClient.Options.trusting().withDefaultTrust())));
            line(out, "  with the demo root trusted, the right name: " + refusedOrNot(TlsClient.refused(port, "/", TlsClient.Options.trusting())));
            line(out, "  with the demo root trusted, but connecting by the address 127.0.0.1: "
                    + refusedOrNot(TlsClient.refused(port, "/", TlsClient.Options.trusting().withHost("127.0.0.1"))) + "  <- THE NAME IN THE CERTIFICATE MUST MATCH");
        }
        try (Nginx nginx = Nginx.start(NginxConfig.Variant.IMPOSTOR)) {
            line(out, "  a server with the right name and a certificate signed by nobody trusted: "
                    + refusedOrNot(TlsClient.refused(nginx.httpsPort(), "/", TlsClient.Options.trusting())));
        }
        try (Nginx nginx = Nginx.start(NginxConfig.Variant.MTLS)) {
            int port = nginx.httpsPort();
            line(out, "  with ssl_verify_client on, a client with no certificate: " + refusedOrNot(TlsClient.refused(port, "/", TlsClient.Options.trusting())));
            TlsClient.Reply ana = TlsClient.get(port, "/", TlsClient.Options.trusting().withClientCertificate());
            line(out, "  with Ana's certificate: " + ana.status() + " and " + ana.body().strip());
            TlsClient.Reply forged = TlsClient.get(port, "/", TlsClient.Options.trusting().withClientCertificate().withHeader("X-Client-DN", "CN=Admin"));
            line(out, "  Ana adds a header claiming to be the admin: " + forged.body().strip() + "  <- NGINX OVERWRITES HEADERS IT SETS ITSELF");
        }
    }

    private static void act5ProxyAndChecks(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FIVE. The proxy trap, a config check and a score.");
        String forged = "GET /app HTTP/1.1\r\nHost: shop.example.test\r\nX-Forwarded-Proto: https\r\nConnection: close\r\n\r\n";
        try (Nginx nginx = Nginx.start(NginxConfig.Variant.PROXY_TRAP)) {
            line(out, "  a plain request claims X-Forwarded-Proto: https and nginx passes it on: " + Probe.send(nginx.httpPort(), forged).body().strip()
                    + "  <- THE APP BELIEVES THE CONNECTION IS SECURE");
        }
        try (Nginx nginx = Nginx.start(NginxConfig.Variant.PROXY_SAFE)) {
            line(out, "  the same request when nginx sets the header from what it saw: " + Probe.send(nginx.httpPort(), forged).body().strip());
        }
        String good = Nginx.configTest(NginxConfig.text(NginxConfig.Variant.CAREFUL));
        line(out, "  nginx -t on the careful configuration: " + (good.contains("syntax is ok") && good.contains("exit code 0") ? "syntax ok" : "failed"));
        String broken = Nginx.configTest(NginxConfig.text(NginxConfig.Variant.CAREFUL, "missing"));
        line(out, "  nginx -t when the certificate file is missing: " + (broken.contains("exit code 1") ? "refused before any customer sees it" : "not caught"));
        line(out, "  the careful configuration scores " + ConfigScore.points(ConfigScore.score(NginxConfig.text(NginxConfig.Variant.CAREFUL))) + " of 7 on the TLS habits");
        line(out, "  a typical copied configuration scores " + ConfigScore.points(ConfigScore.score(SLOPPY)) + " of 7");
        line(out, "  OCSP stapling is for certificates from a public authority and needs the server to reach it, so it is described and not run here");
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
