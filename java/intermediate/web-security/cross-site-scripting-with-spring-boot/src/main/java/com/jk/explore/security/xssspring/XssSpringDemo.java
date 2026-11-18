package com.jk.explore.security.xssspring;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/** Runs the online store story in four numbered acts against a real Spring Boot server on the loopback interface. */
public class XssSpringDemo {

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** A store started on a free port. Closing it stops the server. */
    record Running(ConfigurableApplicationContext context, int port) implements AutoCloseable {

        @Override
        public void close() {
            context.close();
        }
    }

    static Running start() {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(Shop.class).properties(
                "server.port=0",
                "server.address=127.0.0.1",
                "logging.level.root=OFF",
                "spring.main.banner-mode=off",
                "server.servlet.session.cookie.http-only=true",
                "server.servlet.session.cookie.same-site=strict").run();
        int port = Integer.parseInt(context.getEnvironment().getProperty("local.server.port"));
        return new Running(context, port);
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() throws IOException, InterruptedException {
        StringBuilder out = new StringBuilder();
        try (Running shop = start()) {
            Wire.Page page = Wire.get(shop.port(), "/product");
            Wire.Page unescaped = Wire.get(shop.port(), "/product-unescaped");
            act1EscapedByDefault(out, page);
            act2TheTrap(out, unescaped);
            act3OtherPlaces(out, page);
            act4Policy(out, shop, page);
        }
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    static String between(String html, String start, String end) {
        int i = html.indexOf(start);
        if (i < 0) {
            return null;
        }
        int j = html.indexOf(end, i + start.length());
        return j < 0 ? null : html.substring(i + start.length(), j);
    }

    /** The text Thymeleaf wrote for the script value: what follows "const note = " up to the semicolon. */
    static String scriptLiteral(String html) {
        Matcher m = Pattern.compile("const note = (.*?);").matcher(html);
        return m.find() ? m.group(1) : null;
    }

    /** The first line of the text that contains `part`, trimmed. */
    static String lineWith(String text, String part) {
        return text.lines().filter(l -> l.contains(part)).findFirst().orElse("").strip();
    }

    /** The first match of `regex` in `text`. */
    static String find(String text, String regex) {
        Matcher m = Pattern.compile(regex).matcher(text);
        return m.find() ? m.group() : null;
    }

    private static String elements(String html) {
        return Markup.elements(html).stream().map(Markup.Element::name).collect(Collectors.joining(" "));
    }

    private static void act1EscapedByDefault(StringBuilder out, Wire.Page page) {
        line(out, "ONE. Thymeleaf escapes by default.");
        line(out, "  THE VALUE (Ana's review, put in the model):");
        line(out, "    " + ReviewController.REVIEW);
        line(out, "  THE TEMPLATE LINE (product.html):");
        line(out, "    " + lineWith(Templates.text("product"), "${review}"));
        line(out, "  THE PAGE (as the server sent it, status " + page.status() + "):");
        line(out, "    " + lineWith(page.body(), "class=\"review\""));
        line(out, "  THE RESULT (the elements a browser finds):");
        line(out, "    " + elements(page.body()));
        line(out, "    b elements: " + Markup.count(page.body(), "b") + ", so the review stayed text");
        line(out, "  nothing had to be remembered: th:text is the ordinary way");
    }

    private static void act2TheTrap(StringBuilder out, Wire.Page unescaped) {
        line(out, "TWO. th:utext, the one switch that turns escaping off.");
        line(out, "  THE TEMPLATE LINE (product-unescaped.html):");
        line(out, "    " + lineWith(Templates.text("product-unescaped"), "${review}"));
        line(out, "  THE PAGE (as the server sent it, status " + unescaped.status() + "):");
        line(out, "    " + lineWith(unescaped.body(), "class=\"review\""));
        line(out, "  THE RESULT (the elements a browser finds):");
        line(out, "    " + elements(unescaped.body()));
        line(out, "    b elements: " + Markup.count(unescaped.body(), "b") + ", and the shop wrote none  <- THE PAGE CHANGED");
        line(out, "  THE GUARD (a search of the templates for th:utext):");
        for (String name : Templates.NAMES) {
            line(out, String.format(Locale.ROOT, "    %-24s %s", name + ".html", Templates.unescaped().contains(name) ? "uses it" : "no"));
        }
        line(out, "    a test fails if any other template starts to use it");
        line(out, "  th:utext is for markup the shop wrote, never for customer text");
    }

    private static void act3OtherPlaces(StringBuilder out, Wire.Page page) {
        line(out, "THREE. The other places a value can go.");
        String template = Templates.text("product");
        line(out, "  THE VALUES (put in the model):");
        line(out, "    fitNote     " + ReviewController.FIT_NOTE);
        line(out, "    search      " + ReviewController.SEARCH);
        line(out, "    scriptNote  " + ReviewController.SCRIPT_NOTE);
        line(out, "  THE TEMPLATE (how each one is written):");
        line(out, "    " + find(template, "th:attr=\"data-fit=[^\"]*\""));
        line(out, "    " + find(template, "th:href=\"[^\"]*\""));
        line(out, "    th:inline=\"javascript\" and " + find(template, "\\[\\[[^]]*]]"));
        String fit = Markup.attribute(page.body(), "p", "data-fit");
        String href = Markup.attribute(page.body(), "a", "href");
        String literal = scriptLiteral(page.body());
        line(out, "  THE PAGE (what Thymeleaf wrote for each):");
        line(out, "    " + find(page.body(), "data-fit=\"[^\"]*\""));
        line(out, "    " + find(page.body(), "href=\"[^\"]*\""));
        line(out, "    const note = " + literal + ";");
        line(out, "  HOW THE CHARACTERS WERE WRITTEN:");
        line(out, "    \" -> &quot; in an attribute, \\\" in a script string");
        int amp = '&';
        line(out, "    space -> %20, & -> %26 in a link (& is " + amp + " = " + amp / 16 + " x 16 + " + amp % 16 + ")");
        line(out, "  THE RESULT (what the browser reads back):");
        line(out, "    fit note    " + fit + "  (whole: " + ReviewController.FIT_NOTE.equals(fit) + ")");
        line(out, "    link        parameters: " + href.substring(href.indexOf('?') + 1).split("&").length + ", so the & stayed text");
        String inside = literal.substring(1, literal.length() - 1);
        line(out, "    script      one string, no early end: " + !inside.replace("\\\"", "").contains("\""));
    }

    private static void act4Policy(StringBuilder out, Running shop, Wire.Page page) throws IOException, InterruptedException {
        line(out, "FOUR. A strict Content-Security-Policy from Spring Security.");
        String nonce = between(page.policy(), "'nonce-", "'");
        String cookie = page.cookies().stream().filter(c -> c.startsWith("JSESSIONID=")).findFirst().orElse("");
        String id = between(cookie, "JSESSIONID=", ";");
        line(out, "  THE REQUEST:");
        line(out, "    GET /product");
        line(out, "  THE RESPONSE (the nonce shown as <nonce>):");
        line(out, "    status " + page.status() + ", and the header Content-Security-Policy:");
        for (String directive : page.policy().replace(nonce, "<nonce>").split("; ")) {
            line(out, "      " + directive);
        }
        line(out, "    Set-Cookie: " + cookie.replace(id, "<id>"));
        line(out, "    " + find(page.body(), "<script[^>]*>").replace(nonce, "<nonce>"));
        Wire.Page next = Wire.get(shop.port(), "/product");
        int groups = (ContentSecurityPolicy.NONCE_BYTES + 2) / 3;
        line(out, "  HOW THE NONCE GETS THERE (one request):");
        line(out, "    NonceFilter: " + ContentSecurityPolicy.NONCE_BYTES + " random bytes, written in Base64");
        line(out, "    " + ContentSecurityPolicy.NONCE_BYTES + " bytes -> " + groups + " groups of up to 3 -> " + groups * 4 + " characters");
        line(out, "    it runs before Spring Security writes its headers");
        line(out, "    request attribute cspNonce, read by the header and the page");
        line(out, "    this page's nonce: " + nonce.length() + " characters");
        line(out, "    the header's nonce is the script's nonce: " + Markup.scriptNonces(page.body()).equals(List.of(nonce)));
        line(out, "    the next page's nonce differs: " + !next.policy().equals(page.policy()));
        line(out, "  WHICH SCRIPTS MAY RUN (what a browser decides):");
        line(out, "    the shop's script, with the nonce   " + yes(ContentSecurityPolicy.allowsInlineScript(page.policy(), nonce)));
        line(out, "    a script without the nonce          " + yes(ContentSecurityPolicy.allowsInlineScript(page.policy(), null))
                + "  <- BLOCKED");
        line(out, "    the cookie is HttpOnly: " + cookie.contains("HttpOnly") + ", SameSite=Strict: " + cookie.contains("SameSite=Strict"));
    }

    private static String yes(boolean allowed) {
        return allowed ? "yes" : "no";
    }
}
