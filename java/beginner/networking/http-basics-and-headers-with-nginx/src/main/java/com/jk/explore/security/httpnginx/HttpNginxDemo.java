package com.jk.explore.security.httpnginx;

import java.io.IOException;
import java.util.List;

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

    private static String shown(boolean flag) {
        return flag ? "yes" : "no";
    }

    private static void act1Bare(StringBuilder out) throws IOException, InterruptedException {
        line(out, "ONE. nginx in front of the shop, as it comes.");
        try (Nginx nginx = Nginx.start(NginxConfig.text(NginxConfig.Variant.BARE))) {
            Probe.Reply reply = Probe.get(nginx.port(), "/");
            line(out, "  GET / answers " + reply.status() + " and scores " + score(reply) + " of 5 on the security headers");
            boolean version = reply.headers().get("server").matches("nginx/\\d+\\.\\d+\\.\\d+");
            line(out, "  the Server header shows the exact nginx version: " + shown(version) + "  <- A FREE HINT FOR AN ATTACKER");
        }
    }

    private static void act2Always(StringBuilder out) throws IOException, InterruptedException {
        line(out, "TWO. add_header, and the word always.");
        try (Nginx nginx = Nginx.start(NginxConfig.text(NginxConfig.Variant.HEADERS_WITHOUT_ALWAYS))) {
            line(out, "  headers added without always: GET / scores " + score(Probe.get(nginx.port(), "/"))
                    + " of 5, but the 404 page scores " + score(Probe.get(nginx.port(), "/missing")) + " of 5  <- ERROR PAGES ARE LEFT BARE");
        }
        try (Nginx nginx = Nginx.start(NginxConfig.text(NginxConfig.Variant.HEADERS_WITH_ALWAYS))) {
            Probe.Reply page = Probe.get(nginx.port(), "/");
            Probe.Reply missing = Probe.get(nginx.port(), "/missing");
            line(out, "  headers added with always: GET / scores " + score(page) + " of 5 and the 404 page scores " + score(missing) + " of 5");
            line(out, "  with server_tokens off the Server header is just: " + page.headers().get("server"));
        }
    }

    private static void act3Trap(StringBuilder out) throws IOException, InterruptedException {
        line(out, "THREE. The inheritance trap.");
        try (Nginx nginx = Nginx.start(NginxConfig.text(NginxConfig.Variant.INHERITANCE_TRAP))) {
            Probe.Reply top = Probe.get(nginx.port(), "/");
            Probe.Reply account = Probe.get(nginx.port(), "/account");
            line(out, "  headers set once for the whole server: GET / scores " + score(top) + " of 5");
            line(out, "  /account adds its own Cache-Control header, and now scores " + score(account) + " of 5  <- ITS OWN add_header DROPPED THE REST");
            line(out, "  Cache-Control is there: " + shown(account.headers().containsKey("cache-control")));
        }
        try (Nginx nginx = Nginx.start(NginxConfig.text(NginxConfig.Variant.INHERITANCE_FIXED))) {
            line(out, "  with the five headers repeated inside /account it scores " + score(Probe.get(nginx.port(), "/account")) + " of 5");
            line(out, "  the habit: audit every location, not just the top of the page");
        }
    }

    private static String body(int length) {
        return "x".repeat(length);
    }

    private static void act4Limits(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FOUR. Limits, and requests that are ambiguous.");
        try (Nginx nginx = Nginx.start(NginxConfig.text(NginxConfig.Variant.LIMITS))) {
            int port = nginx.port();
            int small = Probe.send(port, post("Content-Length: 100", body(100))).status();
            int big = Probe.send(port, post("Content-Length: 2000", body(2000))).status();
            line(out, "  client_max_body_size 1k: a 100 byte order gets " + small + ", a 2000 byte order gets " + big);
            int header = Probe.send(port, "GET / HTTP/1.1\r\nHost: shop.example\r\nX-Pad: " + body(4000) + "\r\nConnection: close\r\n\r\n").status();
            line(out, "  large_client_header_buffers 2 1k: a header line of 4000 characters gets " + header);
            int both = Probe.send(port, post("Content-Length: 4\r\nTransfer-Encoding: chunked", "0\r\n\r\n")).status();
            line(out, "  Content-Length and Transfer-Encoding together, the smuggling pattern: " + both);
            int twice = Probe.send(port, post("Content-Length: 4\r\nContent-Length: 5", "abcde")).status();
            line(out, "  two different Content-Length headers: " + twice);
            line(out, "  nginx refuses both, so it cannot disagree with the application behind it about where a request ends");
        }
    }

    private static String post(String headers, String body) {
        return "POST /order HTTP/1.1\r\nHost: shop.example\r\n" + headers + "\r\nConnection: close\r\n\r\n" + body;
    }

    private static void act5Upstream(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FIVE. Hiding what the application gives away, and the bill.");
        try (Nginx nginx = Nginx.start(NginxConfig.text(NginxConfig.Variant.UPSTREAM_LEAKY))) {
            Probe.Reply reply = Probe.get(nginx.port(), "/");
            line(out, "  the application behind the proxy sends X-Powered-By and X-AspNet-Version: "
                    + shown(reply.headers().containsKey("x-powered-by") && reply.headers().containsKey("x-aspnet-version")) + "  <- OLD VERSIONS HAVE KNOWN HOLES");
        }
        try (Nginx nginx = Nginx.start(NginxConfig.text(NginxConfig.Variant.UPSTREAM_HIDDEN))) {
            Probe.Reply reply = Probe.get(nginx.port(), "/");
            List<String> leaks = reply.headers().keySet().stream().filter(h -> h.startsWith("x-")).toList();
            line(out, "  with proxy_hide_header and server_tokens off, extra headers left: " + leaks.size() + ", Server says: " + reply.headers().get("server"));
        }
        line(out, "  hiding is a speed bump, not a lock: the real fix is to keep the software up to date");
        line(out, "  the bill: the configuration is easy to get subtly wrong, so test it, as these demos do");
        line(out, "  and the headers describe the page, so a Content-Security-Policy still has to match the real site");
    }
}
