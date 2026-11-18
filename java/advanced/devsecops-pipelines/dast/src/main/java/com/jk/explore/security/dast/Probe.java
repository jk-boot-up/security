package com.jk.explore.security.dast;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * A teaching class: the two active checks of this demo, redone by hand with the same attack strings ZAP sends. Reflected script:
 * send a piece of markup and look for it, unchanged, in the page. SQL injection: send an expression in place of a number; if
 * the page is the one for the number's value, the database worked the expression out, so the input reached the query as code.
 */
final class Probe {

    /** The attack string ZAP's reflected script rule (40012) puts in a parameter. */
    static final String SCRIPT = "</p><scrIpt>alert(1);</scRipt><p>";

    /** The expression ZAP's SQL injection rule (40018) puts in place of id=1: 3-2 is 1 only if a database works it out. */
    static final String EXPRESSION = "3-2";

    /** What came back for one request: the status and the part of the page between the heading and the end of the body. */
    record Answer(int status, String content) {
    }

    private Probe() {
    }

    static Answer get(BankApp app, String path, String param, String value) throws IOException, InterruptedException {
        URI uri = URI.create("http://localhost:" + app.port() + path + "?" + param + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8));
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> r = client.send(HttpRequest.newBuilder(uri).build(), HttpResponse.BodyHandlers.ofString());
            String body = r.body();
            int from = body.indexOf("</h1>");
            int to = body.indexOf("</body>");
            return new Answer(r.statusCode(), from < 0 || to < 0 ? body : body.substring(from + 5, to));
        }
    }

    /** Reflected script: the attack string comes back exactly as it was sent. */
    static boolean reflected(Answer answer) {
        return answer.content().contains(SCRIPT);
    }

    /** SQL injection: the expression gives the same page as the number it works out to, and a different number gives another page. */
    static boolean computed(Answer plain, Answer expression, Answer other) {
        boolean same = plain.content().equals(expression.content());
        boolean another = !plain.content().equals(other.content());
        return plain.status() == 200 && same && another;
    }
}
