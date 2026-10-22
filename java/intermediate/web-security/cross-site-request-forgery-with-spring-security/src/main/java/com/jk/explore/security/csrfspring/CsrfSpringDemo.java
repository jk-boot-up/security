package com.jk.explore.security.csrfspring;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/** Runs the bank story in five numbered acts against real Spring Boot servers on the loopback interface. */
public class CsrfSpringDemo {

    static final Map<String, String> FORGED = Map.of("to", "mallory", "amount", "500");
    static final Map<String, String> HONEST = Map.of("to", "mallory", "amount", "20");

    /** A bank started on a free loopback port with the given profiles. Closing it stops the server. */
    record Running(ConfigurableApplicationContext context, int port) implements AutoCloseable {

        long balance(String account) {
            return context.getBean(Bank.class).balance(account);
        }

        String balances() {
            return "ana " + balance("ana") + ", mallory " + balance("mallory");
        }

        @Override
        public void close() {
            context.close();
        }
    }

    static Running start(String profile, String... properties) {
        String[] all = new String[properties.length + 4];
        System.arraycopy(properties, 0, all, 0, properties.length);
        all[properties.length] = "server.port=0";
        all[properties.length + 1] = "server.address=127.0.0.1";
        all[properties.length + 2] = "logging.level.root=OFF";
        all[properties.length + 3] = "spring.main.banner-mode=off";
        ConfigurableApplicationContext context = new SpringApplicationBuilder(BankApp.class).profiles(profile).properties(all).run();
        return new Running(context, Integer.parseInt(context.getEnvironment().getProperty("local.server.port")));
    }

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. Only statuses, booleans and balances are printed. */
    public static String run() throws IOException, InterruptedException {
        StringBuilder out = new StringBuilder();
        try (Running bank = start("csrf-off")) {
            act1Disabled(out, bank);
        }
        try (Running bank = start("synchronizer")) {
            act2Synchronizer(out, bank);
        }
        try (Running bank = start("spa")) {
            act3SinglePage(out, bank);
        }
        act4ExemptionsAndGet(out);
        act5Bill(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void act1Disabled(StringBuilder out, Running bank) throws IOException, InterruptedException {
        line(out, "ONE. CSRF protection switched off, as many tutorials do to make a POST work.");
        Browser ana = new Browser(bank.port());
        line(out, "  the web chain has http.csrf(c -> c.disable()); ana signs in: " + ana.signIn("ana").status() + ", " + ana.get("/balance").text());
        line(out, "  a page on evil.example posts a transfer; the browser adds ana's session cookie: "
                + ana.postFromAnotherSite("/transfer", FORGED).text() + "  <- A TRANSFER ANA NEVER ASKED FOR");
        line(out, "  balances now: " + bank.balances());
    }

    private static void act2Synchronizer(StringBuilder out, Running bank) throws IOException, InterruptedException {
        line(out, "TWO. Spring Security's default: a token tied to the session.");
        Browser ana = new Browser(bank.port());
        String beforeSignIn = ana.formToken();
        line(out, "  the sign-in form itself needs a token, or anyone could sign a victim in to the attacker's account: without one "
                + ana.signIn("ana").status() + ", with one " + ana.signIn("ana", "X-CSRF-TOKEN", beforeSignIn).status());
        line(out, "  the token is renewed when ana signs in, so the one from before the sign-in no longer works: "
                + ana.post("/transfer", HONEST, "X-CSRF-TOKEN", beforeSignIn).text());
        line(out, "  evil.example posts the same form without a token: " + ana.postFromAnotherSite("/transfer", FORGED).text());
        Browser mallory = new Browser(bank.port());
        mallory.signIn("mallory", "X-CSRF-TOKEN", mallory.formToken());
        line(out, "  with a real token from mallory's own session: " + ana.postFromAnotherSite("/transfer", FORGED, "X-CSRF-TOKEN", mallory.formToken()).text());
        String fresh = ana.formToken();
        line(out, "  ana's own form, with her token: " + ana.post("/transfer", HONEST, "X-CSRF-TOKEN", fresh).text());
        line(out, "  the token is masked with a new random value on every page, so the same token never appears twice (a defence against BREACH): "
                + !ana.formToken().equals(ana.formToken()));
        line(out, "  and every masked token is accepted: " + ana.post("/transfer", HONEST, "X-CSRF-TOKEN", ana.formToken()).status());
        line(out, "  logging out is a state change too: POST /logout without a token " + ana.post("/logout", Map.of()).status()
                + ", with one " + ana.post("/logout", Map.of(), "X-CSRF-TOKEN", ana.formToken()).status());
        line(out, "  balances now: " + bank.balances());
    }

    private static void act3SinglePage(StringBuilder out, Running bank) throws IOException, InterruptedException {
        line(out, "THREE. A single-page app: the token in a cookie that the page's script reads.");
        Browser ana = new Browser(bank.port());
        ana.get("/csrf");
        ana.signIn("ana", "X-XSRF-TOKEN", ana.cookie("XSRF-TOKEN").getValue());
        ana.get("/csrf");
        HttpCookie cookie = ana.cookie("XSRF-TOKEN");
        line(out, "  the XSRF-TOKEN cookie is set, and HttpOnly is " + cookie.isHttpOnly() + ": the script has to read it, so a script injected by cross-site scripting can too");
        line(out, "  evil.example posts without the header: " + ana.postFromAnotherSite("/transfer", FORGED).text());
        Browser mallory = new Browser(bank.port());
        mallory.get("/csrf");
        line(out, "  with the value from mallory's own cookie: "
                + ana.postFromAnotherSite("/transfer", FORGED, "X-XSRF-TOKEN", mallory.cookie("XSRF-TOKEN").getValue()).text());
        line(out, "  the bank's own script copies the cookie into the X-XSRF-TOKEN header: "
                + ana.post("/transfer", HONEST, "X-XSRF-TOKEN", cookie.getValue()).text());
        line(out, "  evil.example cannot do that copy: a browser lets a page read only its own site's cookies");
        line(out, "  balances now: " + bank.balances());
    }

    private static void act4ExemptionsAndGet(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FOUR. What the default does not cover.");
        try (Running bank = start("synchronizer")) {
            Browser ana = new Browser(bank.port());
            ana.signIn("ana", "X-CSRF-TOKEN", ana.formToken());
            line(out, "  the payment provider's webhook is exempt from tokens, because it has no session: a forged post with ana's cookie and no signature: "
                    + ana.postFromAnotherSite("/webhooks/payments", Map.of()).text());
            line(out, "  the exemption is safe only because the route checks its own proof: with the signature, "
                    + ana.post("/webhooks/payments", Map.of(), "X-Signature", BankController.WEBHOOK_SECRET).text());
            line(out, "  a route that moves money on GET, from a link on evil.example: "
                    + ana.getFromAnotherSite("/legacy/move?to=mallory&amount=100").text() + "  <- SPRING DOES NOT CHECK GET FOR A TOKEN");
            line(out, "  the same transfer to a route that needs POST: " + ana.getFromAnotherSite("/transfer?to=mallory&amount=100").status() + "  <- GET MUST NEVER CHANGE STATE");
            line(out, "  balances now: " + bank.balances());
        }
        try (Running bank = start("synchronizer", "csrf.ignore=/webhooks/**,/transfer")) {
            Browser ana = new Browser(bank.port());
            ana.signIn("ana", "X-CSRF-TOKEN", ana.formToken());
            line(out, "  a developer adds /transfer to the exemptions to make a test pass; evil.example posts: "
                    + ana.postFromAnotherSite("/transfer", FORGED).text() + "  <- ONE LINE OF CONFIGURATION UNDID THE DEFENCE");
        }
    }

    private static void act5Bill(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FIVE. The bill.");
        try (Running bank = start("synchronizer")) {
            Browser ana = new Browser(bank.port());
            String cookie = String.join(" | ", ana.signIn("ana", "X-CSRF-TOKEN", ana.formToken()).setCookies());
            line(out, "  the session cookie by default: HttpOnly " + cookie.contains("HttpOnly") + ", SameSite " + cookie.contains("SameSite"));
            line(out, "  the API chain takes credentials in a header and keeps no session: ana's cookie from evil.example, no header: "
                    + ana.postFromAnotherSite("/api/transfer", FORGED).text());
            line(out, "  the app that holds the credentials: " + Browser.apiPost(bank.port(), "ana", "/api/transfer", HONEST).text());
            line(out, "  so the API chain can leave CSRF off, because a browser never adds a header it was not given");
            int uncovered = 0;
            List<String> routes = List.of("/transfer", "/logout", "/login");
            for (String route : routes) {
                if (new Browser(bank.port()).postFromAnotherSite(route, Map.of()).status() != 403) {
                    uncovered++;
                }
            }
            line(out, "  every state-changing route of the web chain, posted from evil.example with no token: " + (routes.size() - uncovered) + " of "
                    + routes.size() + " refused with 403");
        }
        try (Running bank = start("synchronizer", "server.servlet.session.cookie.same-site=lax")) {
            Browser ana = new Browser(bank.port());
            String cookie = String.join(" | ", ana.signIn("ana", "X-CSRF-TOKEN", ana.formToken()).setCookies());
            line(out, "  with server.servlet.session.cookie.same-site=lax: SameSite=Lax is " + cookie.contains("SameSite=Lax")
                    + "; it is one more layer, and the token is still needed");
        }
    }
}
