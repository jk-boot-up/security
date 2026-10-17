package com.jk.explore.security.http;

import java.io.IOException;
import java.util.List;

/** Runs the online store story in five numbered acts and prints exact, repeatable results. */
public class HttpDemo {

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() throws IOException, InterruptedException, BadMessageException {
        StringBuilder out = new StringBuilder();
        act1Message(out);
        act2Ambiguity(out);
        act3Injection(out);
        act4Cookies(out);
        act5Headers(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void act1Message(StringBuilder out) throws BadMessageException {
        line(out, "ONE. An HTTP message is just text.");
        HttpRequest request = HttpRequest.of("POST", "/orders", "shop.example", "item=A-1001&qty=2");
        String wire = request.serialize();
        line(out, "  a customer's browser sends " + wire.length() + " characters: a request line, headers, a blank line, and a body");
        HttpRequest parsed = HttpParser.parse(wire);
        line(out, "  parsed: method " + parsed.method() + ", target " + parsed.target() + ", Host " + parsed.header("Host")
                + ", body " + parsed.body().length + " bytes");
        line(out, "  written out again it is the same text: " + parsed.serialize().equals(wire));
        line(out, "  every header is only a line of text that the sender chose, so the server must check it");
    }

    private static String tryParse(String raw) {
        try {
            HttpParser.parse(raw);
            return "accepted";
        } catch (BadMessageException e) {
            return "refused (" + e.getMessage() + ")";
        }
    }

    private static void act2Ambiguity(StringBuilder out) {
        line(out, "TWO. Messages that two programs could read differently.");
        String base = "POST /orders HTTP/1.1\r\nHost: shop.example\r\n";
        line(out, "  Content-Length and Transfer-Encoding together: "
                + tryParse(base + "Content-Length: 4\r\nTransfer-Encoding: chunked\r\n\r\n0\r\n\r\n"));
        line(out, "  two different Content-Length headers: " + tryParse(base + "Content-Length: 4\r\nContent-Length: 9\r\n\r\nabcd"));
        line(out, "  a space before the colon in a header name: " + tryParse(base + "Content-Length : 4\r\n\r\nabcd"));
        line(out, "  a body longer than its Content-Length, with a second request hidden after it: "
                + tryParse(base + "Content-Length: 4\r\n\r\nabcdGET /admin HTTP/1.1\r\nHost: shop.example\r\n\r\n"));
        line(out, "  the same request, plain and exact: " + tryParse(base + "Content-Length: 4\r\n\r\nabcd"));
        line(out, "  when a proxy and a server disagree about where a request ends, the rest is read as the next customer's request");
    }

    private static String trySerialize(HttpResponse response) {
        try {
            response.serialize();
            return "written";
        } catch (BadMessageException e) {
            return "refused (" + e.getMessage() + ")";
        }
    }

    private static void act3Injection(StringBuilder out) {
        line(out, "THREE. Header injection.");
        String attack = "shoes\r\nSet-Cookie: session=attacker";
        line(out, "  the store puts the customer's search text in a redirect address");
        line(out, "  a normal search, shoes: " + trySerialize(HttpResponse.redirectToSearch("shoes")));
        line(out, "  search text with a line break and a second header: " + trySerialize(HttpResponse.redirectToSearch(attack))
                + "  <- WITHOUT THE CHECK THE ATTACKER SETS A COOKIE");
        line(out, "  the answer is to refuse or encode line breaks in anything that reaches a header");
    }

    private static void act4Cookies(StringBuilder out) throws BadMessageException {
        line(out, "FOUR. Cookies that carry a login.");
        String[] cookies = {
            "session=abc123",
            "session=abc123; Secure; HttpOnly; SameSite=Lax; Path=/",
            "session=abc123; HttpOnly; SameSite=None",
            "__Host-session=abc123; Secure; HttpOnly; SameSite=Strict; Path=/; Domain=shop.example"
        };
        for (String text : cookies) {
            List<String> problems = Cookie.parse(text).problems();
            line(out, "  " + text);
            line(out, "    problems: " + problems.size());
            for (String problem : problems) {
                line(out, "    - " + problem);
            }
        }
    }

    private static void act5Headers(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FIVE. Security headers on a real response.");
        LoopbackExchange.Result plain = LoopbackExchange.fetch(java.util.Map.of());
        List<HeaderAudit.Finding> plainFindings = HeaderAudit.audit(plain.headers());
        line(out, "  a real request to a plain server: status " + plain.status() + ", body '" + plain.body() + "'");
        line(out, "  security headers present: " + HeaderAudit.score(plainFindings) + " of 5");
        LoopbackExchange.Result hardened = LoopbackExchange.fetch(LoopbackExchange.HARDENED);
        List<HeaderAudit.Finding> findings = HeaderAudit.audit(hardened.headers());
        line(out, "  the same request to a server that sets the five headers: " + HeaderAudit.score(findings) + " of 5");
        for (HeaderAudit.Finding finding : findings) {
            line(out, "    " + finding.header() + ": " + (finding.passed() ? "ok" : "FAIL") + " (" + finding.reason() + ")");
        }
        List<HeaderAudit.Finding> weak = HeaderAudit.audit(java.util.Map.of("Strict-Transport-Security", "max-age=300",
                "Content-Security-Policy", "script-src 'unsafe-inline'", "X-Content-Type-Options", "nosniff",
                "X-Frame-Options", "ALLOWALL", "Referrer-Policy", "unsafe-url"));
        line(out, "  headers present but set badly score " + HeaderAudit.score(weak) + " of 5: a header is only worth its value");
    }
}
