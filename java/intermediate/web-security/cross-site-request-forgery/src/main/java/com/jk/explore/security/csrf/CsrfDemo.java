package com.jk.explore.security.csrf;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Runs the online bank story in five numbered acts and prints exact, repeatable results. Tokens and session ids are random and never printed. */
public class CsrfDemo {

    static final String BANK = Bank.ORIGIN;
    static final String EVIL = "https://evil.example";
    static final String REVIEWS = "https://reviews.bank.example";
    static final String TRANSFER = BANK + "/transfer";
    static final Map<String, String> TO_MALLORY = Map.of("to", "mallory", "amount", "500");
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
            Browser ana = ana(server, false);
            line(out, "  Ana logs in at the bank, and her browser keeps the session cookie: balance " + server.bank().balance("ana"));
            line(out, "  in another tab she opens a page on evil.example, whose hidden form posts to the bank: send 500 to mallory");
            Response r = ana.submit(EVIL, "POST", TRANSFER, TO_MALLORY);
            line(out, "  the bank answers: " + shown(r) + "  <- A TRANSFER ANA NEVER ASKED FOR");
            line(out, "  balances now: ana " + server.bank().balance("ana") + ", mallory " + server.bank().balance("mallory"));
            line(out, "  the browser added Ana's cookie because the request went to the bank; the cookie proves who, not what she meant");
        }
    }

    private static void act2Tokens(StringBuilder out) throws IOException, InterruptedException {
        line(out, "TWO. A token in every form.");
        try (BankServer server = new BankServer(new Bank(Protection.none().withTokens()))) {
            Browser ana = ana(server, false);
            String token = token(ana);
            line(out, "  the bank's own transfer form carries a token tied to Ana's session: " + (token != null));
            Response own = ana.submit(BANK, "POST", TRANSFER, Map.of("to", "mallory", "amount", "20", "csrf", token));
            line(out, "  Ana sends 20 with the bank's form: " + shown(own));
            line(out, "  the page on evil.example posts the same form without a token: " + shown(ana.submit(EVIL, "POST", TRANSFER, TO_MALLORY)));
            Browser mallory = loggedIn(server, false, "mallory", "mallory-demo-password");
            String his = token(mallory);
            Response stolen = ana.submit(EVIL, "POST", TRANSFER, Map.of("to", "mallory", "amount", "500", "csrf", his));
            line(out, "  with a real token from Mallory's own session: " + shown(stolen));
            line(out, "  evil.example cannot read Ana's token: the form lives on the bank's pages, and a browser does not let another site read them");
            line(out, "  the token is compared in constant time, so the time an answer takes does not reveal how much of a guess was right");
            line(out, "  balances now: ana " + server.bank().balance("ana") + ", mallory " + server.bank().balance("mallory"));
        }
    }

    private static void act3SameSite(StringBuilder out) throws IOException, InterruptedException {
        line(out, "THREE. SameSite cookies.");
        try (BankServer server = new BankServer(new Bank(Protection.none().withSameSite("Lax")))) {
            Browser ana = ana(server, false);
            line(out, "  SameSite=Lax: the forged post from evil.example arrives without the cookie: " + shown(ana.submit(EVIL, "POST", TRANSFER, TO_MALLORY)));
            line(out, "  SameSite=Lax: a link on evil.example to the bank still opens Ana's account: " + shown(ana.click(EVIL, BANK + "/account")));
            line(out, "  SameSite=Lax: the bank's own form still works: "
                    + shown(ana.submit(BANK, "POST", TRANSFER, Map.of("to", "mallory", "amount", "20"))));
        }
        try (BankServer server = new BankServer(new Bank(Protection.none().withSameSite("Strict")))) {
            Browser ana = ana(server, false);
            line(out, "  SameSite=Strict: the same link from evil.example arrives without the cookie: " + shown(ana.click(EVIL, BANK + "/account"))
                    + ", so Ana looks logged out");
            line(out, "  SameSite=Strict: typing the bank's address works: " + shown(ana.visit(BANK + "/account")));
        }
        try (BankServer server = new BankServer(new Bank(Protection.none().withSameSite(null)))) {
            Browser ana = ana(server, false);
            line(out, "  no SameSite at all: a modern browser treats the cookie as Lax: " + shown(ana.submit(EVIL, "POST", TRANSFER, TO_MALLORY)));
        }
    }

    private static void act4WhereItCameFrom(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FOUR. Where the request came from, and GET.");
        try (BankServer server = new BankServer(new Bank(Protection.none().withFetchChecks()))) {
            Browser ana = ana(server, false);
            line(out, "  the browser says Sec-Fetch-Site: cross-site, and the bank refuses: " + shown(ana.submit(EVIL, "POST", TRANSFER, TO_MALLORY)));
            line(out, "  the bank's own form says same-origin: " + shown(ana.submit(BANK, "POST", TRANSFER, Map.of("to", "mallory", "amount", "20"))));
            Browser old = ana(server, true);
            line(out, "  an older browser sends no Sec-Fetch-Site, so the bank reads Origin: " + shown(old.submit(EVIL, "POST", TRANSFER, TO_MALLORY)));
            line(out, "  with neither header, only the token is left: these checks add depth, they do not replace it");
        }
        try (BankServer server = new BankServer(new Bank(Protection.none().withSameSite("Lax").withGetTransfers()))) {
            Browser ana = ana(server, false);
            line(out, "  a bank that moves money on GET, with a Lax cookie: a link on evil.example: "
                    + shown(ana.click(EVIL, LINK_TO_MALLORY)) + "  <- GET MUST NEVER CHANGE STATE");
            line(out, "  Lax lets cookies ride on top-level links, which is safe only because a link should never act");
        }
        try (BankServer server = new BankServer(new Bank(Protection.none().withSameSite("Lax")))) {
            Browser ana = ana(server, false);
            line(out, "  the same link to a bank that needs POST: " + shown(ana.click(EVIL, LINK_TO_MALLORY)));
        }
    }

    private static void act5TheBill(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FIVE. The bill.");
        try (BankServer server = new BankServer(new Bank(Protection.none().withSameSite("Lax")))) {
            Browser old = ana(server, true);
            line(out, "  SameSite as the only defence, in an older browser that ignores it: " + shown(old.submit(EVIL, "POST", TRANSFER, TO_MALLORY)));
        }
        try (BankServer server = new BankServer(new Bank(Protection.none().withSameSite("Strict")))) {
            Browser ana = ana(server, false);
            line(out, "  SameSite=Strict, and a page on reviews.bank.example, where customers post content: "
                    + shown(ana.submit(REVIEWS, "POST", TRANSFER, TO_MALLORY)) + "  <- SAME SITE, SO THE COOKIE IS SENT");
        }
        try (BankServer server = new BankServer(new Bank(Protection.none().withSameSite("Strict").withTokens()))) {
            Browser ana = ana(server, false);
            line(out, "  the same page, with tokens switched on: " + shown(ana.submit(REVIEWS, "POST", TRANSFER, TO_MALLORY)));
            line(out, "  a bank page that forgot to include the token refuses Ana herself: "
                    + shown(ana.submit(BANK, "POST", TRANSFER, Map.of("to", "mallory", "amount", "20"))) + ", so the token must reach every form and client");
        }
        try (BankServer server = new BankServer(new Bank(Protection.none()))) {
            Browser ana = ana(server, false);
            String apiToken = server.bank().issueApiToken("ana");
            line(out, "  the bank's API takes a token in an Authorization header; evil.example's script cannot add a header it does not know: "
                    + shown(ana.fetch(EVIL, "POST", BANK + "/api/transfer", TO_MALLORY, Map.of())));
            line(out, "  Ana's own app, which holds the token: "
                    + shown(ana.fetch(BANK, "POST", BANK + "/api/transfer", Map.of("to", "mallory", "amount", "20"), Map.of("Authorization", "Bearer " + apiToken))));
            line(out, "  so APIs with bearer tokens are not exposed the same way, as long as the token is not kept in a cookie");
        }
    }
}
