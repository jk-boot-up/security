package com.jk.explore.security.csrf;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runs the online bank story in five numbered acts and prints exact, repeatable results: each request as the browser sent it, and
 * the bank's response, in separate labelled sections. Tokens and session ids are random, so they are printed as placeholders.
 */
public class CsrfDemo {

    static final String BANK = Bank.ORIGIN;
    static final String EVIL = "https://evil.example";
    static final String REVIEWS = "https://reviews.bank.example";
    static final String TRANSFER = BANK + "/transfer";
    static final Map<String, String> TO_MALLORY = Map.of("to", "mallory", "amount", "500");
    static final Map<String, String> TWENTY = Map.of("to", "mallory", "amount", "20");
    static final String LINK_TO_MALLORY = BANK + "/transfer?to=mallory&amount=100";

    private static final Pattern TOKEN = Pattern.compile("name=\"csrf\" value=\"([^\"]+)\"");

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() throws IOException, InterruptedException {
        StringBuilder out = new StringBuilder();
        act1TheProblem(out);
        act2Tokens(out);
        act3SameSite(out);
        act4WhereItCameFrom(out);
        act5TheBill(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static String shown(Response r) {
        return r.status() + " " + r.body();
    }

    private static String balances(Bank bank) {
        return "balance: ana " + bank.balance("ana") + ", mallory " + bank.balance("mallory");
    }

    /** A random value is printed as a placeholder, so the output is the same on every run. */
    static String masked(String text, String id) {
        return text.replaceAll("sid=[^;&]+", "sid=" + id).replaceAll("csrf=[^&]+", "csrf=<token>");
    }

    /** Whether the browser attached the cookie to its last request. */
    private static String cookie(Browser browser) {
        return browser.lastSent().headers().containsKey("Cookie") ? "sent" : "not sent";
    }

    /** The last request exactly as the browser sent it: the line, the form fields and the headers the bank reads. */
    private static void request(StringBuilder out, Browser browser) {
        Browser.Sent s = browser.lastSent();
        line(out, "    " + s.method() + " " + s.url());
        if (!s.body().isEmpty()) {
            line(out, "    " + masked(s.body(), "<id>"));
        }
        for (String h : new String[] {"Cookie", "Origin", "Sec-Fetch-Site"}) {
            if (s.headers().containsKey(h)) {
                line(out, "    " + h + ": " + masked(s.headers().get(h), "<Ana's id>"));
            }
        }
    }

    static Browser loggedIn(BankServer server, boolean legacy, String user, String password) throws IOException, InterruptedException {
        Browser browser = new Browser(server, legacy);
        browser.submit(BANK, "POST", BANK + "/login", Map.of("user", user, "password", password));
        return browser;
    }

    static Browser ana(BankServer server, boolean legacy) throws IOException, InterruptedException {
        return loggedIn(server, legacy, "ana", "correct horse battery staple");
    }

    static String token(Browser browser) throws IOException, InterruptedException {
        Matcher m = TOKEN.matcher(browser.visit(BANK + "/transfer-form").body());
        return m.find() ? m.group(1) : null;
    }

    private static void act1TheProblem(StringBuilder out) throws IOException, InterruptedException {
        line(out, "ONE. A request Ana did not intend.");
        try (BankServer server = new BankServer(new Bank(Protection.none()))) {
            Browser ana = new Browser(server, false);
            Response login = ana.submit(BANK, "POST", BANK + "/login", Map.of("user", "ana", "password", "correct horse battery staple"));
            line(out, "  ANA LOGS IN (POST " + BANK + "/login):");
            line(out, "    " + shown(login));
            line(out, "    Set-Cookie: " + masked(login.headers().get("Set-Cookie"), "<id>"));
            line(out, "    " + balances(server.bank()));
            Response r = ana.submit(EVIL, "POST", TRANSFER, TO_MALLORY);
            line(out, "  THE REQUEST (a hidden form on " + EVIL + "):");
            request(out, ana);
            line(out, "  THE RESPONSE:");
            line(out, "    " + shown(r) + "  <- ANA NEVER ASKED FOR");
            line(out, "    " + balances(server.bank()));
            line(out, "  the browser added the cookie because the request went");
            line(out, "  to the bank: it proves who asks, not what Ana meant");
        }
    }

    private static void act2Tokens(StringBuilder out) throws IOException, InterruptedException {
        line(out, "TWO. A token in every form.");
        try (BankServer server = new BankServer(new Bank(Protection.none().withTokens()))) {
            Browser ana = ana(server, false);
            String token = token(ana);
            Browser mallory = loggedIn(server, false, "mallory", "mallory-demo-password");
            String his = token(mallory);
            int bits = Bank.TOKEN_BYTES * 8;
            line(out, "  THE BANK'S FORM (GET /transfer-form, as Ana):");
            line(out, "    <input type=\"hidden\" name=\"csrf\" value=\"<Ana's token>\">");
            line(out, "  HOW THE TOKEN IS MADE:");
            line(out, "    " + Bank.TOKEN_BYTES + " random bytes from SecureRandom = " + bits + " bits");
            line(out, "    Base64 URL text, 6 bits a character: " + bits + " / 6 = "
                    + String.format("%.1f", bits / 6.0) + " -> " + (int) Math.ceil(bits / 6.0));
            line(out, "    Ana's token: " + token.length() + " characters, kept in her session");
            Response own = ana.submit(BANK, "POST", TRANSFER, Map.of("to", "mallory", "amount", "20", "csrf", token));
            Response none = ana.submit(EVIL, "POST", TRANSFER, TO_MALLORY);
            Response stolen = ana.submit(EVIL, "POST", TRANSFER, Map.of("to", "mallory", "amount", "500", "csrf", his));
            line(out, "  THE REQUESTS (POST /transfer, each with Ana's cookie):");
            line(out, "    1 the bank's form  csrf=<Ana's token>      amount=20");
            line(out, "    2 evil.example     no csrf field           amount=500");
            line(out, "    3 evil.example     csrf=<Mallory's token>  amount=500");
            line(out, "  THE CHECK (the sent token against Ana's session token):");
            line(out, "    1 " + compare(token, token));
            line(out, "    2 " + compare(null, token));
            line(out, "    3 " + compare(his, token) + ": it was made for Mallory");
            line(out, "  THE RESPONSES:");
            line(out, "    1 " + shown(own));
            line(out, "    2 " + shown(none));
            line(out, "    3 " + shown(stolen));
            line(out, "    " + balances(server.bank()));
            line(out, "  evil.example cannot read Ana's form: a browser does not let");
            line(out, "  one site read another site's pages");
        }
    }

    /** The bank's token check, redone here to print its steps: missing, or equal in constant time. */
    static String compare(String sent, String session) {
        if (sent == null) {
            return "missing";
        }
        boolean equal = MessageDigest.isEqual(sent.getBytes(StandardCharsets.UTF_8), session.getBytes(StandardCharsets.UTF_8));
        return equal ? "equal (MessageDigest.isEqual, in constant time)" : "not equal";
    }

    private static void act3SameSite(StringBuilder out) throws IOException, InterruptedException {
        line(out, "THREE. SameSite cookies.");
        line(out, "  HOW THE BROWSER FINDS THE SITE (the last two labels):");
        line(out, "    " + EVIL + "  -> " + Site.registrable(Site.host(EVIL)));
        line(out, "    " + BANK + "  -> " + Site.registrable(Site.host(BANK)));
        line(out, "    the sites differ, so the request is "
                + (Site.sameSite(EVIL, TRANSFER) ? "same-site" : "cross-site"));
        line(out, "  THE RULE FOR A CROSS-SITE REQUEST:");
        line(out, "    None: the cookie goes, if it is also Secure");
        line(out, "    Lax: the cookie goes only on a top-level GET link");
        line(out, "    Strict: the cookie never goes");
        try (BankServer server = new BankServer(new Bank(Protection.none().withSameSite("Lax")))) {
            Browser ana = ana(server, false);
            Response forged = ana.submit(EVIL, "POST", TRANSFER, TO_MALLORY);
            String c1 = cookie(ana);
            Response link = ana.click(EVIL, BANK + "/account");
            String c2 = cookie(ana);
            Response own = ana.submit(BANK, "POST", TRANSFER, TWENTY);
            String c3 = cookie(ana);
            line(out, "  THE REQUESTS (Ana's cookie has SameSite=Lax):");
            line(out, "    1 a forged POST from evil.example  cookie: " + c1);
            line(out, "    2 a link (GET) on evil.example     cookie: " + c2);
            line(out, "    3 the bank's own form (POST)       cookie: " + c3);
            line(out, "  THE RESPONSES:");
            line(out, "    1 " + shown(forged));
            line(out, "    2 " + shown(link));
            line(out, "    3 " + shown(own));
        }
        try (BankServer server = new BankServer(new Bank(Protection.none().withSameSite("Strict")))) {
            Browser ana = ana(server, false);
            Response link = ana.click(EVIL, BANK + "/account");
            String c4 = cookie(ana);
            Response typed = ana.visit(BANK + "/account");
            String c5 = cookie(ana);
            line(out, "  THE REQUESTS (SameSite=Strict):");
            line(out, "    4 a link (GET) on evil.example     cookie: " + c4);
            line(out, "    5 Ana types the bank's address     cookie: " + c5);
            line(out, "  THE RESPONSES:");
            line(out, "    4 " + shown(link) + ", so Ana looks logged out");
            line(out, "    5 " + shown(typed));
        }
        try (BankServer server = new BankServer(new Bank(Protection.none().withSameSite(null)))) {
            Browser ana = ana(server, false);
            Response forged = ana.submit(EVIL, "POST", TRANSFER, TO_MALLORY);
            line(out, "  THE REQUEST (no SameSite: a modern browser reads it as Lax):");
            line(out, "    6 a forged POST from evil.example  cookie: " + cookie(ana));
            line(out, "  THE RESPONSE:");
            line(out, "    6 " + shown(forged));
        }
    }

    private static void act4WhereItCameFrom(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FOUR. Where the request came from, and GET.");
        line(out, "  HOW THE BROWSER PICKS Sec-Fetch-Site (a page -> bank.example):");
        for (String page : new String[] {BANK, REVIEWS, EVIL}) {
            String how = Site.origin(page).equals(BANK) ? "same origin" : Site.sameSite(page, TRANSFER) ? "same site" : "other site";
            line(out, String.format("    %-30s%-12s -> %s", page, how, Site.fetchSite(page, TRANSFER)));
        }
        try (BankServer server = new BankServer(new Bank(Protection.none().withFetchChecks()))) {
            Browser ana = ana(server, false);
            Response forged = ana.submit(EVIL, "POST", TRANSFER, TO_MALLORY);
            String h1 = "Sec-Fetch-Site: " + ana.lastSent().headers().get("Sec-Fetch-Site");
            Response own = ana.submit(BANK, "POST", TRANSFER, TWENTY);
            String h2 = "Sec-Fetch-Site: " + ana.lastSent().headers().get("Sec-Fetch-Site");
            Browser old = ana(server, true);
            Response legacy = old.submit(EVIL, "POST", TRANSFER, TO_MALLORY);
            String h3 = "Origin: " + old.lastSent().headers().get("Origin");
            line(out, "  THE REQUESTS (POST /transfer; the bank reads these headers):");
            line(out, "    1 forged, modern browser  " + h1);
            line(out, "    2 the bank's own form     " + h2);
            line(out, "    3 forged, older browser   " + h3);
            line(out, "  THE RESPONSES:");
            line(out, "    1 " + shown(forged));
            line(out, "    2 " + shown(own));
            line(out, "    3 " + shown(legacy));
            line(out, "  with neither header, only the token is left");
        }
        try (BankServer server = new BankServer(new Bank(Protection.none().withSameSite("Lax").withGetTransfers()))) {
            Browser ana = ana(server, false);
            Response moved = ana.click(EVIL, LINK_TO_MALLORY);
            line(out, "  THE REQUEST (a link on evil.example; the cookie is Lax):");
            request(out, ana);
            line(out, "  THE RESPONSES:");
            line(out, "    a bank that moves money on GET:");
            line(out, "      " + shown(moved) + "  <- A LINK MOVED MONEY");
        }
        try (BankServer server = new BankServer(new Bank(Protection.none().withSameSite("Lax")))) {
            Browser ana = ana(server, false);
            line(out, "    a bank that needs POST for a transfer:");
            line(out, "      " + shown(ana.click(EVIL, LINK_TO_MALLORY)));
        }
    }

    private static void act5TheBill(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FIVE. The bill.");
        Response oldBrowser;
        Response subdomain;
        Response withTokens;
        Response forgotten;
        try (BankServer server = new BankServer(new Bank(Protection.none().withSameSite("Lax")))) {
            oldBrowser = ana(server, true).submit(EVIL, "POST", TRANSFER, TO_MALLORY);
        }
        try (BankServer server = new BankServer(new Bank(Protection.none().withSameSite("Strict")))) {
            subdomain = ana(server, false).submit(REVIEWS, "POST", TRANSFER, TO_MALLORY);
        }
        try (BankServer server = new BankServer(new Bank(Protection.none().withSameSite("Strict").withTokens()))) {
            Browser ana = ana(server, false);
            withTokens = ana.submit(REVIEWS, "POST", TRANSFER, TO_MALLORY);
            forgotten = ana.submit(BANK, "POST", TRANSFER, TWENTY);
        }
        line(out, "  THE REQUESTS (POST /transfer):");
        line(out, "    1 older browser, SameSite=Lax only, from evil.example");
        line(out, "    2 SameSite=Strict only, from reviews.bank.example");
        line(out, "    3 the same as 2, with tokens switched on");
        line(out, "    4 Ana, from a bank page that forgot the token");
        line(out, "  THE RESPONSES:");
        line(out, "    1 " + shown(oldBrowser) + "  <- SameSite IS IGNORED");
        line(out, "    2 " + shown(subdomain) + "  <- SAME SITE: COOKIE SENT");
        line(out, "    3 " + shown(withTokens));
        line(out, "    4 " + shown(forgotten) + "  <- ANA HERSELF IS REFUSED");
        try (BankServer server = new BankServer(new Bank(Protection.none()))) {
            Browser ana = ana(server, false);
            String apiToken = server.bank().issueApiToken("ana");
            Response script = ana.fetch(EVIL, "POST", BANK + "/api/transfer", TO_MALLORY, Map.of());
            Response app = ana.fetch(BANK, "POST", BANK + "/api/transfer", TWENTY, Map.of("Authorization", "Bearer " + apiToken));
            line(out, "  THE API REQUESTS (POST /api/transfer):");
            line(out, "    5 a script on evil.example, no Authorization header");
            line(out, "    6 Ana's app: Authorization: Bearer <API token>");
            line(out, "  THE RESPONSES:");
            line(out, "    5 " + shown(script));
            line(out, "    6 " + shown(app));
            line(out, "  a browser never adds that header by itself, so a bearer");
            line(out, "  token is safe from CSRF unless it is kept in a cookie");
        }
    }
}
