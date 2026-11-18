package com.jk.explore.security.csrfspring;

import java.io.IOException;
import java.net.HttpCookie;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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

    /** A Set-Cookie header with the random value replaced, so the output is the same on every run. */
    static String masked(List<String> setCookies, String name) {
        String placeholder = name.equals("JSESSIONID") ? "=<id>" : "=<token>";
        return setCookies.stream().filter(c -> c.startsWith(name + "=")).findFirst()
                .map(c -> c.replaceFirst("=[^;]*", placeholder)).orElse("(none)");
    }

    private static void act1Disabled(StringBuilder out, Running bank) throws IOException, InterruptedException {
        line(out, "ONE. CSRF protection switched off, as many tutorials do.");
        Browser ana = new Browser(bank.port());
        line(out, "  THE CONFIGURATION (the web chain):");
        line(out, "    http.csrf(c -> c.disable())");
        Browser.Reply signIn = ana.signIn("ana");
        line(out, "  ANA SIGNS IN (POST /login):");
        line(out, "    " + signIn.status() + ", Set-Cookie: " + masked(signIn.setCookies(), "JSESSIONID"));
        line(out, "    GET /balance -> " + ana.get("/balance").text());
        Browser.Reply forged = ana.postFromAnotherSite("/transfer", FORGED);
        line(out, "  THE REQUEST (a hidden form on https://evil.example):");
        line(out, "    POST /transfer   amount=500&to=mallory");
        line(out, "    Cookie: " + ana.cookie("JSESSIONID").getName() + "=<Ana's session>  (the browser adds it)");
        line(out, "    Origin: https://evil.example");
        line(out, "  THE RESPONSE:");
        line(out, "    " + forged.text() + "  <- ANA NEVER ASKED FOR");
        line(out, "    balance: " + bank.balances());
    }

    private static void act2Synchronizer(StringBuilder out, Running bank) throws IOException, InterruptedException {
        line(out, "TWO. Spring Security's default: a token tied to the session.");
        line(out, "  THE CONFIGURATION (the web chain):");
        line(out, "    http.csrf(csrf -> csrf");
        line(out, "        .ignoringRequestMatchers(\"/webhooks/**\"))");
        Browser ana = new Browser(bank.port());
        String beforeSignIn = ana.formToken();
        int without = ana.signIn("ana").status();
        int with = ana.signIn("ana", "X-CSRF-TOKEN", beforeSignIn).status();
        line(out, "  THE SIGN-IN FORM NEEDS A TOKEN TOO (no forged sign-in):");
        line(out, "    POST /login, no token -> " + without);
        line(out, "    POST /login, with a token -> " + with);
        Browser.Reply old = ana.post("/transfer", HONEST, "X-CSRF-TOKEN", beforeSignIn);
        Browser.Reply none = ana.postFromAnotherSite("/transfer", FORGED);
        Browser mallory = new Browser(bank.port());
        mallory.signIn("mallory", "X-CSRF-TOKEN", mallory.formToken());
        Browser.Reply his = ana.postFromAnotherSite("/transfer", FORGED, "X-CSRF-TOKEN", mallory.formToken());
        Browser.Reply own = ana.post("/transfer", HONEST, "X-CSRF-TOKEN", ana.formToken());
        line(out, "  THE REQUESTS (POST /transfer, each with Ana's cookie):");
        line(out, "    1 Ana, with the token from before she signed in");
        line(out, "    2 evil.example, no token");
        line(out, "    3 evil.example, a real token from Mallory's session");
        line(out, "    4 Ana's own form, with her token (amount=20)");
        line(out, "  THE RESPONSES:");
        line(out, "    1 " + old.text() + "  (the token is renewed at sign-in)");
        line(out, "    2 " + none.text());
        line(out, "    3 " + his.text());
        line(out, "    4 " + own.text());
        String page1 = ana.formToken();
        String page2 = ana.formToken();
        String token = TokenMask.unmask(page1);
        int bytes = page1.length() / 4 * 3;
        line(out, "  HOW THE TOKEN IS MASKED (a new mask on every page):");
        line(out, "    the session's token: a UUID of " + token.length() + " characters: "
                + token.matches("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}"));
        line(out, "    " + token.length() + " random bytes + (token XOR random) = " + bytes + " bytes");
        line(out, "    as Base64 URL text: " + bytes + " / 3 * 4 = " + page1.length() + " characters");
        line(out, "    page 1 and page 2 differ: " + !page1.equals(page2));
        line(out, "    first half XOR second half: the same token on both: "
                + (TokenMask.unmask(page2).equals(token)));
        line(out, "  ONE BYTE OF IT ('a' and a random byte 0x5c):");
        byte[] sent = TokenMask.xor(new byte[] {'a'}, new byte[] {0x5c});
        byte[] back = TokenMask.xor(sent, new byte[] {0x5c});
        line(out, String.format("    0x61 XOR 0x5c = 0x%02x is sent", sent[0]));
        line(out, String.format("    0x%02x XOR 0x5c = 0x%02x ('%c') again", sent[0], back[0], (char) back[0]));
        Browser.Reply byHand = ana.post("/transfer", HONEST, "X-CSRF-TOKEN", TokenMask.mask(token, new byte[token.length()]));
        Browser.Reply logoutWithout = ana.post("/logout", Map.of());
        Browser.Reply logoutWith = ana.post("/logout", Map.of(), "X-CSRF-TOKEN", ana.formToken());
        line(out, "  THE REQUESTS (POST, each with Ana's cookie):");
        line(out, "    5 /transfer, the token masked by hand (amount=20)");
        line(out, "    6 /logout, no token");
        line(out, "    7 /logout, with her token");
        line(out, "  THE RESPONSES:");
        line(out, "    5 " + byHand.text());
        line(out, "    6 " + logoutWithout.status() + "    7 " + logoutWith.status() + "  (logging out is a state change too)");
        line(out, "    balance: " + bank.balances());
    }

    private static void act3SinglePage(StringBuilder out, Running bank) throws IOException, InterruptedException {
        line(out, "THREE. A single-page app: the token in a readable cookie.");
        line(out, "  THE CONFIGURATION (the web chain):");
        line(out, "    csrf.csrfTokenRepository(");
        line(out, "        CookieCsrfTokenRepository.withHttpOnlyFalse())");
        Browser ana = new Browser(bank.port());
        Browser.Reply first = ana.get("/csrf");
        ana.signIn("ana", "X-XSRF-TOKEN", ana.cookie("XSRF-TOKEN").getValue());
        ana.get("/csrf");
        HttpCookie cookie = ana.cookie("XSRF-TOKEN");
        line(out, "  THE COOKIE THE BANK SETS:");
        line(out, "    Set-Cookie: " + masked(first.setCookies(), "XSRF-TOKEN"));
        line(out, "    HttpOnly: " + cookie.isHttpOnly() + ", so the page's script can read it,");
        line(out, "    and so could injected script (cross-site scripting)");
        Browser.Reply none = ana.postFromAnotherSite("/transfer", FORGED);
        Browser mallory = new Browser(bank.port());
        mallory.get("/csrf");
        Browser.Reply his = ana.postFromAnotherSite("/transfer", FORGED, "X-XSRF-TOKEN", mallory.cookie("XSRF-TOKEN").getValue());
        Browser.Reply own = ana.post("/transfer", HONEST, "X-XSRF-TOKEN", cookie.getValue());
        line(out, "  THE REQUESTS (POST /transfer; the browser adds both cookies):");
        line(out, "    1 evil.example, no X-XSRF-TOKEN header");
        line(out, "    2 evil.example, the header = Mallory's cookie value");
        line(out, "    3 the bank's script copies the cookie into the header");
        line(out, "  THE RESPONSES (the header must equal the cookie):");
        line(out, "    1 " + none.text());
        line(out, "    2 " + his.text());
        line(out, "    3 " + own.text());
        line(out, "    balance: " + bank.balances());
        line(out, "  evil.example cannot copy the cookie: a page reads only");
        line(out, "  its own site's cookies");
    }

    private static void act4ExemptionsAndGet(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FOUR. What the default does not cover.");
        try (Running bank = start("synchronizer")) {
            Browser ana = new Browser(bank.port());
            ana.signIn("ana", "X-CSRF-TOKEN", ana.formToken());
            Browser.Reply webhook = ana.postFromAnotherSite("/webhooks/payments", Map.of());
            Browser.Reply signed = ana.post("/webhooks/payments", Map.of(), "X-Signature", BankController.WEBHOOK_SECRET);
            Browser.Reply legacy = ana.getFromAnotherSite("/legacy/move?to=mallory&amount=100");
            Browser.Reply post = ana.getFromAnotherSite("/transfer?to=mallory&amount=100");
            line(out, "  THE REQUESTS (Ana is signed in; no CSRF token is sent):");
            line(out, "    1 evil.example: POST /webhooks/payments, no signature");
            line(out, "    2 the payment provider: the same, with X-Signature");
            line(out, "    3 evil.example link: GET /legacy/move?to=mallory&amount=100");
            line(out, "    4 evil.example link: GET /transfer?to=mallory&amount=100");
            line(out, "  THE RESPONSES:");
            line(out, "    1 " + webhook.text() + "  (no token needed: it checks a signature)");
            line(out, "    2 " + signed.text());
            line(out, "    3 " + legacy.text() + "  <- GET IS NEVER CHECKED");
            line(out, "    4 " + post.status() + "  (the route needs POST)");
            line(out, "    balance: " + bank.balances());
        }
        try (Running bank = start("synchronizer", "csrf.ignore=/webhooks/**,/transfer")) {
            Browser ana = new Browser(bank.port());
            ana.signIn("ana", "X-CSRF-TOKEN", ana.formToken());
            line(out, "  THE CONFIGURATION, AFTER ONE CHANGE TO PASS A TEST:");
            line(out, "    csrf.ignore=/webhooks/**,/transfer");
            line(out, "  THE REQUEST: evil.example, POST /transfer, no token");
            line(out, "  THE RESPONSE:");
            line(out, "    " + ana.postFromAnotherSite("/transfer", FORGED).text() + "  <- DEFENCE UNDONE");
        }
    }

    private static void act5Bill(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FIVE. The bill.");
        try (Running bank = start("synchronizer")) {
            Browser ana = new Browser(bank.port());
            List<String> cookies = ana.signIn("ana", "X-CSRF-TOKEN", ana.formToken()).setCookies();
            String session = masked(cookies, "JSESSIONID");
            line(out, "  THE SESSION COOKIE BY DEFAULT:");
            line(out, "    Set-Cookie: " + session);
            line(out, "    HttpOnly: " + session.contains("HttpOnly") + "    SameSite set: " + session.contains("SameSite"));
            String basic = Base64.getEncoder().encodeToString(("ana:" + WebSecurity.PASSWORD).getBytes(StandardCharsets.UTF_8));
            Browser.Reply forged = ana.postFromAnotherSite("/api/transfer", FORGED);
            Browser.Reply app = Browser.apiPost(bank.port(), "ana", "/api/transfer", HONEST);
            line(out, "  THE API REQUESTS (POST /api/transfer; no session at all):");
            line(out, "    1 evil.example: Ana's cookie, no Authorization header");
            line(out, "    2 Ana's app: Authorization: Basic " + basic);
            line(out, "      (Base64 of ana:" + WebSecurity.PASSWORD + ")");
            line(out, "  THE RESPONSES:");
            line(out, "    1 " + forged.text());
            line(out, "    2 " + app.text());
            line(out, "  a browser never adds a header it was not given");
            StringBuilder each = new StringBuilder();
            int refused = 0;
            List<String> routes = List.of("/transfer", "/logout", "/login");
            for (String route : routes) {
                int status = new Browser(bank.port()).postFromAnotherSite(route, Map.of()).status();
                each.append(route).append(' ').append(status).append("   ");
                refused += status == 403 ? 1 : 0;
            }
            line(out, "  EACH STATE-CHANGING ROUTE (evil.example, no token):");
            line(out, "    " + each.toString().strip());
            line(out, "    " + refused + " of " + routes.size() + " refused");
        }
        try (Running bank = start("synchronizer", "server.servlet.session.cookie.same-site=lax")) {
            Browser ana = new Browser(bank.port());
            List<String> cookies = ana.signIn("ana", "X-CSRF-TOKEN", ana.formToken()).setCookies();
            line(out, "  WITH server.servlet.session.cookie.same-site=lax:");
            String[] parts = masked(cookies, "JSESSIONID").split(" (?=SameSite)");
            line(out, "    Set-Cookie: " + parts[0]);
            line(out, "                " + parts[1]);
            line(out, "    one more layer; the token is still needed");
        }
    }
}
