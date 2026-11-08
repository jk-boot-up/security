package com.jk.explore.security.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Runs the online store story in five numbered acts and prints exact, repeatable results. Each act prints the input
 * (THE REQUEST, THE SEARCH TEXT, THE COOKIES...) and the result (THE PARSED REQUEST, THE VERDICT...) as separate sections.
 */
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

    /** A line as it travels, with the two invisible line-break characters written out as \r\n. */
    private static String visible(String text) {
        return text.replace("\r", "\\r").replace("\n", "\\n");
    }

    private static void act1Message(StringBuilder out) throws BadMessageException {
        line(out, "ONE. An HTTP message is just text.");
        HttpRequest request = HttpRequest.of("POST", "/orders", "shop.example", "item=A-1001&qty=2");
        String wire = request.serialize();
        line(out, "  THE REQUEST (the text the browser sends, line by line):");
        String head = wire.substring(0, wire.indexOf("\r\n\r\n") + 4);
        for (String text : head.split("(?<=\r\n)")) {
            int chars = text.length() - 2;
            line(out, String.format("    %-26s %2d + 2 = %2d", visible(text), chars, text.length()));
        }
        String body = wire.substring(head.length());
        line(out, String.format("    %-26s %2d     = %2d", body, body.length(), body.length()));
        line(out, String.format("    %-26s          %d characters", "total", wire.length()));
        HttpRequest parsed = HttpParser.parse(wire);
        line(out, "  THE PARSED REQUEST:");
        line(out, "    method   " + parsed.method());
        line(out, "    target   " + parsed.target());
        line(out, "    Host     " + parsed.header("Host"));
        line(out, "    body     " + parsed.body().length + " bytes, as Content-Length said");
        line(out, "  written out again it is the same text: " + parsed.serialize().equals(wire));
    }

    private static String tryParse(String raw) {
        try {
            HttpParser.parse(raw);
            return "accepted";
        } catch (BadMessageException e) {
            return "refused: " + e.getMessage();
        }
    }

    private static void act2Ambiguity(StringBuilder out) {
        line(out, "TWO. Messages that two programs could read differently.");
        String base = "POST /orders HTTP/1.1\r\nHost: shop.example\r\n";
        String hidden = "GET /admin HTTP/1.1\r\nHost: shop.example\r\n\r\n";
        line(out, "  THE REQUEST (Content-Length: 4, then more after the body):");
        line(out, String.format("    body     %-26s %2d bytes", "abcd", 4));
        StringBuilder sum = new StringBuilder("4");
        for (String text : hidden.split("(?<=\r\n)")) {
            line(out, String.format("    hidden   %-26s %2d bytes", visible(text), text.length()));
            sum.append(" + ").append(text.length());
        }
        line(out, "    after the blank line: " + sum + " = " + (4 + hidden.length()) + " bytes");
        line(out, "  THE PARSER'S ANSWER:");
        line(out, "    " + tryParse(base + "Content-Length: 4\r\n\r\nabcd" + hidden));
        line(out, "    a proxy that trusts Content-Length stops after 4 bytes");
        line(out, "    and reads the other " + hidden.length() + " as the next customer's request");
        line(out, "  MORE MESSAGES THAT TWO PROGRAMS COULD READ DIFFERENTLY:");
        line(out, "    Content-Length and Transfer-Encoding together");
        line(out, "      " + tryParse(base + "Content-Length: 4\r\nTransfer-Encoding: chunked\r\n\r\n0\r\n\r\n"));
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.of("tr"));
            line(out, "    TRANSFER-ENCODING on a machine set to Turkish");
            line(out, "      Turkish lower case: " + "TRANSFER-ENCODING".toLowerCase() + " (dotless i)");
            line(out, "      " + tryParse(base + "Content-Length: 4\r\nTRANSFER-ENCODING: chunked\r\n\r\n0\r\n\r\n"));
        } finally {
            Locale.setDefault(original);
        }
        line(out, "    two different Content-Length headers");
        line(out, "      " + tryParse(base + "Content-Length: 4\r\nContent-Length: 9\r\n\r\nabcd"));
        line(out, "    a space before the colon: 'Content-Length : 4'");
        line(out, "      " + tryParse(base + "Content-Length : 4\r\n\r\nabcd"));
        line(out, "    the same request, plain and exact");
        line(out, "      " + tryParse(base + "Content-Length: 4\r\n\r\nabcd"));
    }

    private static String trySerialize(HttpResponse response) {
        try {
            String text = response.serialize();
            return "written: " + text.split("\r\n")[1];
        } catch (BadMessageException e) {
            return "refused: " + e.getMessage();
        }
    }

    private static void act3Injection(StringBuilder out) {
        line(out, "THREE. Header injection.");
        String attack = "shoes\r\nSet-Cookie: session=attacker";
        line(out, "  THE SEARCH TEXT (typed by the customer):");
        line(out, "    normal   shoes");
        line(out, "    attack   " + visible(attack));
        line(out, "  BEFORE THE FIX (the attack, put in the redirect as it is):");
        String unchecked = "HTTP/1.1 302 Found\r\nLocation: /search?q=" + attack + "\r\n";
        for (String text : unchecked.split("\r\n")) {
            line(out, "    " + text + (text.startsWith("Set-Cookie") ? "  <- WRITTEN BY THE ATTACKER" : ""));
        }
        line(out, "  AFTER THE FIX (line breaks refused when writing a header):");
        line(out, "    normal   " + trySerialize(HttpResponse.redirectToSearch("shoes")));
        line(out, "    attack   " + trySerialize(HttpResponse.redirectToSearch(attack)));
    }

    /** The score worked out: one point per header that passed, added up, e.g. "0 + 0 + 1 + 0 + 0 = 1 of 5". */
    private static String points(List<HeaderAudit.Finding> findings) {
        List<String> points = new ArrayList<>();
        for (HeaderAudit.Finding finding : findings) {
            points.add(finding.passed() ? "1" : "0");
        }
        return String.join(" + ", points) + " = " + HeaderAudit.score(findings) + " of 5";
    }

    /** A header's value, whatever the case of its name: the client gives names in lower case. */
    private static String valueOf(Map<String, String> headers, String name) {
        return headers.entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue).findFirst().orElse("(missing)");
    }

    /** Splits a long Set-Cookie value after a semicolon, so each printed line stays short. */
    private static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String part : text.split("(?<=; )")) {
            if (current.length() + part.length() > width && current.length() > 0) {
                lines.add(current.toString().strip());
                current.setLength(0);
            }
            current.append(part);
        }
        lines.add(current.toString().strip());
        return lines;
    }

    private static void act4Cookies(StringBuilder out) throws BadMessageException {
        line(out, "FOUR. Cookies that carry a login.");
        String[] cookies = {
            "session=abc123",
            "session=abc123; Secure; HttpOnly; SameSite=Lax; Path=/",
            "session=abc123; HttpOnly; SameSite=None",
            "__Host-session=abc123; Secure; HttpOnly; SameSite=Strict; Path=/; Domain=shop.example"
        };
        for (int group = 0; group < cookies.length; group += 2) {     // two cookies per group, so each part stays short
            line(out, "  THE COOKIES (what the store sends in Set-Cookie):");
            for (int i = group; i < group + 2; i++) {
                List<String> lines = wrap(cookies[i], 44);
                for (int j = 0; j < lines.size(); j++) {
                    line(out, (j == 0 ? "    cookie " + (i + 1) + "  " : "              ") + lines.get(j));
                }
            }
            line(out, "  THE VERDICT (what is wrong, if it carries a login):");
            for (int i = group; i < group + 2; i++) {
                List<String> problems = Cookie.parse(cookies[i]).problems();
                line(out, "    cookie " + (i + 1) + "  " + problems.size() + (problems.size() == 1 ? " problem" : " problems"));
                for (String problem : problems) {
                    line(out, "      - " + problem);
                }
            }
        }
    }

    private static void act5Headers(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FIVE. Security headers on a real response.");
        LoopbackExchange.Result plain = LoopbackExchange.fetch(Map.of());
        LoopbackExchange.Result hardened = LoopbackExchange.fetch(LoopbackExchange.HARDENED);
        line(out, "  THE RESPONSES (a real server and client on this machine):");
        line(out, "    plain server     status " + plain.status() + ", body '" + plain.body() + "'");
        line(out, "    hardened server  status " + hardened.status() + ", the same body, and these five:");
        for (Map.Entry<String, String> header : LoopbackExchange.HARDENED.entrySet()) {
            line(out, String.format("    %-26s%s", header.getKey(), valueOf(hardened.headers(), header.getKey())));
        }
        line(out, "  THE AUDIT (one point per header):");
        line(out, "    plain server     " + points(HeaderAudit.audit(plain.headers())));
        line(out, "    hardened server  " + points(HeaderAudit.audit(hardened.headers())));
        Map<String, String> weak = new LinkedHashMap<>();
        weak.put("Strict-Transport-Security", "max-age=300");
        weak.put("Content-Security-Policy", "script-src 'unsafe-inline'");
        weak.put("X-Content-Type-Options", "nosniff");
        weak.put("X-Frame-Options", "ALLOWALL");
        weak.put("Referrer-Policy", "unsafe-url");
        line(out, "  HEADERS SET BADLY (all five present):");
        for (Map.Entry<String, String> header : weak.entrySet()) {
            line(out, String.format("    %-26s%s", header.getKey(), header.getValue()));
        }
        List<HeaderAudit.Finding> weakFindings = HeaderAudit.audit(weak);
        line(out, "  THE AUDIT, HEADER BY HEADER:");
        for (HeaderAudit.Finding finding : weakFindings) {
            line(out, String.format("    %-26s%-5s%s", finding.header(), finding.passed() ? "ok" : "FAIL", finding.reason()));
        }
        line(out, "    score " + points(weakFindings));
        line(out, "  a header is only worth its value");
    }
}
