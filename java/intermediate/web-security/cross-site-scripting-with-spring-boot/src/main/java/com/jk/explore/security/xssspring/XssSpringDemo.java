package com.jk.explore.security.xssspring;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/** Runs the online store story in five numbered acts against a real Spring Boot server on the loopback interface. */
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
            act5TheBill(out, page);
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

    private static void act1EscapedByDefault(StringBuilder out, Wire.Page page) {
        line(out, "ONE. Thymeleaf escapes by default.");
        line(out, "  Ana's review is the text " + ReviewController.REVIEW + ", written with th:text");
        line(out, "  the page answers " + page.status() + ", and the review is written as: " + between(page.body(), "<p class=\"review\">", "</p>"));
        line(out, "  bold elements on the page: " + Markup.count(page.body(), "b") + ", so the review stayed text");
        line(out, "  nothing had to be remembered: th:text is the ordinary way to write a value");
    }

    private static void act2TheTrap(StringBuilder out, Wire.Page unescaped) {
        line(out, "TWO. th:utext, the one switch that turns escaping off.");
        line(out, "  the same review written with th:utext: " + between(unescaped.body(), "<p class=\"review\">", "</p>"));
        line(out, "  bold elements on the page: " + Markup.count(unescaped.body(), "b") + ", and the shop wrote none  <- THE REVIEW BECAME PART OF THE PAGE");
        line(out, "  th:utext means unescaped text: it is for markup the shop wrote itself, never for anything a customer typed");
        line(out, "  a search of the templates for th:utext finds every place this can happen: " + Templates.usingUnescapedText() + " of " + Templates.NAMES.size()
                + " templates, and a test fails if another one appears");
    }

    private static void act3OtherPlaces(StringBuilder out, Wire.Page page) {
        line(out, "THREE. The other places a value can go.");
        String fit = Markup.attribute(page.body(), "p", "data-fit");
        line(out, "  in an attribute, with th:attr: Ben's fit note reads back whole: " + fit + "  (" + ReviewController.FIT_NOTE.equals(fit) + ")");
        String href = Markup.attribute(page.body(), "a", "href");
        line(out, "  in a link, with @{/search(q=...)}: " + href);
        line(out, "  the search text is one parameter, and its & did not start a second one: " + (href.substring(href.indexOf('?') + 1).split("&").length == 1));
        line(out, "  in a script, with th:inline=\"javascript\": const note = " + scriptLiteral(page.body()));
        line(out, "  each value went through the escaping for its own place, chosen by how it was written in the template");
    }

    private static void act4Policy(StringBuilder out, Running shop, Wire.Page page) throws IOException, InterruptedException {
        line(out, "FOUR. A strict Content-Security-Policy from Spring Security, with a fresh nonce for every page.");
        line(out, "  the product page answers with a policy: " + (page.policy() != null));
        String nonce = page.policy() == null ? null : between(page.policy(), "'nonce-", "'");
        line(out, "  the policy names a nonce, and the shop's own script carries the same one: " + Markup.scriptNonces(page.body()).equals(java.util.List.of(nonce)));
        Wire.Page next = Wire.get(shop.port(), "/product");
        line(out, "  the next page has a different nonce: " + !next.policy().equals(page.policy()));
        line(out, "  the shop's script may run: " + ContentSecurityPolicy.allowsInlineScript(page.policy(), nonce));
        line(out, "  a script that slipped in without the nonce may run: " + ContentSecurityPolicy.allowsInlineScript(page.policy(), null)
                + "  <- A SECOND LINE OF DEFENCE");
        line(out, "  the nonce comes from a filter that runs before Spring Security writes its headers, so the header and the page always agree");
    }

    private static void act5TheBill(StringBuilder out, Wire.Page page) {
        line(out, "FIVE. The bill.");
        String cookie = page.cookies().stream().filter(c -> c.startsWith("JSESSIONID=")).findFirst().orElse("");
        line(out, "  the session cookie is HttpOnly: " + cookie.contains("HttpOnly") + ", and SameSite=Strict: " + cookie.contains("SameSite=Strict"));
        line(out, "  escaping by default protects only the values written through it: th:utext, and HTML built in Java code, skip it");
        line(out, "  a value placed in an event handler or a style needs more than escaping: keep customer text out of those places");
        line(out, "  Spring Security writes a fixed policy unless the application supplies the nonce, as this store does with one small filter");
    }
}
