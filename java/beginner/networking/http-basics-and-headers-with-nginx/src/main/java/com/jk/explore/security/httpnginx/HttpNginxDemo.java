package com.jk.explore.security.httpnginx;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/** Runs the online store story in five numbered acts against real nginx containers and prints repeatable results. */
public class HttpNginxDemo {

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text. It needs a running Docker, so its test is tagged "docker". */
    public static String run() throws IOException, InterruptedException {
        StringBuilder out = new StringBuilder();
        act1Bare(out);
        act2Always(out);
        act3Trap(out);
        act4Limits(out);
        act5Upstream(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    static int score(Probe.Reply reply) {
        return HeaderAudit.score(HeaderAudit.audit(reply.headers()));
    }

    /** One point per header, added up in the open: "0 + 1 + 1 + 1 + 1 = 4 of 5". */
    static String points(Probe.Reply reply) {
        List<HeaderAudit.Finding> findings = HeaderAudit.audit(reply.headers());
        StringBuilder sum = new StringBuilder();
        for (HeaderAudit.Finding finding : findings) {
            sum.append(sum.length() == 0 ? "" : " + ").append(finding.passed() ? 1 : 0);
        }
        return sum + " = " + HeaderAudit.score(findings) + " of 5";
    }

    private static String pad(String text, int width) {
        return String.format("%-" + width + "s", text);
    }

    /** A header of the reply, or "missing". */
    private static String value(Probe.Reply reply, String name) {
        return reply.headers().getOrDefault(name.toLowerCase(Locale.ROOT), "missing");
    }

    private static void act1Bare(StringBuilder out) throws IOException, InterruptedException {
        line(out, "ONE. nginx in front of the shop, as it comes.");
        try (Nginx nginx = Nginx.start(NginxConfig.text(NginxConfig.Variant.BARE))) {
            line(out, "  THE REQUEST (to nginx with its default settings):");
            line(out, "    GET / HTTP/1.1");
            Probe.Reply reply = Probe.get(nginx.port(), "/");
            line(out, "  THE RESPONSE (status " + reply.status() + "):");
            line(out, "    " + pad("Server", 27) + value(reply, "Server") + "   <- THE EXACT VERSION");
            for (String[] header : NginxConfig.HEADERS) {
                line(out, "    " + pad(header[0], 27) + value(reply, header[0]));
            }
            line(out, "  THE AUDIT (one point per security header):");
            line(out, "    " + points(reply));
            line(out, "  the version number is a free hint: an attacker looks up");
            line(out, "  the known holes of exactly that release");
        }
    }

    private static void act2Always(StringBuilder out) throws IOException, InterruptedException {
        line(out, "TWO. add_header, and the word always.");
        String[] frame = NginxConfig.HEADERS.get(3);
        line(out, "  THE FIVE HEADERS (each is one add_header line):");
        for (String[] header : NginxConfig.HEADERS) {
            String[] parts = header[1].split("; ");
            for (int i = 0; i < parts.length; i++) {
                line(out, "    " + pad(i == 0 ? header[0] : "", 27) + parts[i] + (i < parts.length - 1 ? ";" : ""));
            }
        }
        line(out, "  THE CONFIGURATION (two versions of each of the five lines):");
        line(out, "    without  add_header " + frame[0] + " \"" + frame[1] + "\";");
        line(out, "    with     add_header " + frame[0] + " \"" + frame[1] + "\" always;");
        line(out, "  THE RESPONSES (a page, and a page that does not exist):");
        try (Nginx nginx = Nginx.start(NginxConfig.text(NginxConfig.Variant.HEADERS_WITHOUT_ALWAYS))) {
            Probe.Reply page = Probe.get(nginx.port(), "/");
            Probe.Reply missing = Probe.get(nginx.port(), "/missing");
            line(out, "    without  GET /         " + page.status() + "  " + points(page));
            line(out, "    without  GET /missing  " + missing.status() + "  " + points(missing));
        }
        try (Nginx nginx = Nginx.start(NginxConfig.text(NginxConfig.Variant.HEADERS_WITH_ALWAYS))) {
            Probe.Reply page = Probe.get(nginx.port(), "/");
            Probe.Reply missing = Probe.get(nginx.port(), "/missing");
            line(out, "    with     GET /         " + page.status() + "  " + points(page));
            line(out, "    with     GET /missing  " + missing.status() + "  " + points(missing));
            line(out, "  without always, only 200, 201, 204, 206, 301, 302,");
            line(out, "  303, 304, 307 and 308 answers get the headers");
            line(out, "  with server_tokens off the Server header is just: " + page.headers().get("server"));
        }
    }

    private static void act3Trap(StringBuilder out) throws IOException, InterruptedException {
        line(out, "THREE. The inheritance trap.");
        line(out, "  THE CONFIGURATION (the five headers set once, for the server):");
        line(out, "    server {");
        line(out, "      add_header " + NginxConfig.HEADERS.get(0)[0] + " ... always;");
        line(out, "      ... and the other four, the same way");
        line(out, "      location = /account {");
        line(out, "        add_header Cache-Control \"no-store\" always;");
        line(out, "  THE RESPONSES:");
        try (Nginx nginx = Nginx.start(NginxConfig.text(NginxConfig.Variant.INHERITANCE_TRAP))) {
            Probe.Reply top = Probe.get(nginx.port(), "/");
            Probe.Reply account = Probe.get(nginx.port(), "/account");
            line(out, "    GET /          " + points(top));
            line(out, "    GET /account   " + points(account) + "  <- DROPPED");
            line(out, "                   Cache-Control: " + value(account, "Cache-Control"));
        }
        line(out, "  its own add_header replaces every add_header of the server");
        try (Nginx nginx = Nginx.start(NginxConfig.text(NginxConfig.Variant.INHERITANCE_FIXED))) {
            line(out, "  AFTER THE FIX (the five repeated inside /account):");
            line(out, "    GET /account   " + points(Probe.get(nginx.port(), "/account")));
        }
        line(out, "  the habit: audit every location, not just the top page");
    }

    private static String body(int length) {
        return "x".repeat(length);
    }

    private static void act4Limits(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FOUR. Limits, and requests that are ambiguous.");
        line(out, "  THE CONFIGURATION:");
        line(out, "    client_max_body_size 1k;           1k = 1024 bytes");
        line(out, "    large_client_header_buffers 2 1k;  per header line");
        String padding = "X-Pad: " + body(4000);
        line(out, "  THE REQUESTS (sent as raw bytes):");
        line(out, "    a  an order with a body of 100 bytes");
        line(out, "    b  an order with a body of 2000 bytes");
        line(out, "    c  one header line of " + padding.length() + " characters");
        line(out, "    d  Content-Length: 4 and Transfer-Encoding: chunked");
        line(out, "    e  Content-Length: 4 and Content-Length: 5");
        try (Nginx nginx = Nginx.start(NginxConfig.text(NginxConfig.Variant.LIMITS))) {
            int port = nginx.port();
            int small = Probe.send(port, post("Content-Length: 100", body(100))).status();
            int big = Probe.send(port, post("Content-Length: 2000", body(2000))).status();
            int header = Probe.send(port, "GET / HTTP/1.1\r\nHost: shop.example\r\n" + padding + "\r\nConnection: close\r\n\r\n").status();
            int both = Probe.send(port, post("Content-Length: 4\r\nTransfer-Encoding: chunked", "0\r\n\r\n")).status();
            int twice = Probe.send(port, post("Content-Length: 4\r\nContent-Length: 5", "abcde")).status();
            line(out, "  THE RESPONSES:");
            line(out, "    a  " + small + "  100 <= 1024 bytes: accepted");
            line(out, "    b  " + big + "  2000 > 1024 bytes: the body is too large");
            line(out, "    c  " + header + "  " + padding.length() + " > 1024: the line fits no buffer");
            line(out, "    d  " + both + "  two ways to say where the body ends");
            line(out, "    e  " + twice + "  two different lengths");
            line(out, "  nginx refuses d and e, so it cannot disagree with the");
            line(out, "  application behind it about where a request ends");
        }
    }

    private static String post(String headers, String body) {
        return "POST /order HTTP/1.1\r\nHost: shop.example\r\n" + headers + "\r\nConnection: close\r\n\r\n" + body;
    }

    private static void leaks(StringBuilder out, Probe.Reply reply) {
        line(out, "    " + pad("Server", 18) + value(reply, "Server"));
        for (String name : List.of("X-Powered-By", "X-AspNet-Version")) {
            String v = value(reply, name);
            line(out, "    " + pad(name, 18) + (v.equals("missing") ? "hidden" : v));
        }
    }

    private static void act5Upstream(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FIVE. Hiding what the application gives away, and the bill.");
        try (Nginx nginx = Nginx.start(NginxConfig.text(NginxConfig.Variant.UPSTREAM_LEAKY))) {
            line(out, "  THE RESPONSE (the application's headers, passed on by nginx):");
            leaks(out, Probe.get(nginx.port(), "/"));
        }
        try (Nginx nginx = Nginx.start(NginxConfig.text(NginxConfig.Variant.UPSTREAM_HIDDEN))) {
            line(out, "  AFTER THE FIX (proxy_hide_header, server_tokens off):");
            leaks(out, Probe.get(nginx.port(), "/"));
        }
        line(out, "  hiding is a speed bump, not a lock: the real fix is to keep");
        line(out, "  the software up to date");
        line(out, "  THE BILL:");
        line(out, "    the configuration is easy to get subtly wrong: test it");
        line(out, "    a Content-Security-Policy still has to match the real site");
    }
}
