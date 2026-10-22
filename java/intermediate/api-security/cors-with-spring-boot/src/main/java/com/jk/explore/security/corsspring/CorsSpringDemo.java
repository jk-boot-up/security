package com.jk.explore.security.corsspring;

import java.io.IOException;
import java.util.List;

/** Runs the bank API story in five numbered acts against real Spring Boot servers on the loopback interface. */
public class CorsSpringDemo {

    static final String PARTNER = "https://partner.example";
    static final String EVIL = "https://evil.example";
    static final String LOOKALIKE = "https://evilbank.example";
    static final String OPAQUE = "null";

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. Only statuses, booleans and balances are printed; ports never are. */
    public static String run() throws IOException, InterruptedException {
        StringBuilder out = new StringBuilder();
        act1NoPolicy(out);
        act2LooksLikeFixes(out);
        act3Exact(out);
        act4Where(out);
        act5Bill(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String label, Object result) {
        out.append("%-40s -> %s%n".formatted(label, result));
    }

    private static void act1NoPolicy(StringBuilder out) throws IOException, InterruptedException {
        out.append("== 1. No policy: a page may send, but not read ==\n");
        try (Running bank = Running.start("none", "security")) {
            Browser b = bank.browser();
            line(out, "bank's own page reads /account", b.fetch(bank.url(), "GET", "/account", null, true));
            int ownPage = bank.processed().list().size();
            line(out, "evil.example reads /account", b.fetch(EVIL, "GET", "/account", null, true));
            line(out, "evil.example POSTs /transfer (simple)", b.fetch(EVIL, "POST", "/transfer", "text/plain", true));
            line(out, "evil.example PUTs /transfer", b.fetch(EVIL, "PUT", "/transfer", "application/json", true));
            line(out, "requests the bank processed", bank.processed().list().subList(ownPage, bank.processed().list().size()) + "  <- CORS DID NOT STOP THE POST");
        }
    }

    private static void act2LooksLikeFixes(StringBuilder out) throws IOException, InterruptedException {
        out.append("\n== 2. Policies that look like fixes ==\n");
        try (Running bank = Running.start("star-with-credentials", "mvc")) {
            Browser.Reply r = bank.browser().raw("GET", "/account", EVIL, true);
            line(out, "allowedOrigins(\"*\") with credentials", "Spring answers the first cross-origin request with " + r.status() + " and sends no Allow-Origin header: " + (r.header("access-control-allow-origin") == null));
        }
        policy(out, "allowedOriginPatterns(\"*\") with credentials", "reflect");
        policy(out, "exact list that also allows null", "exact-and-null");
        policy(out, "pattern https://*bank.example", "suffix");
    }

    private static void policy(StringBuilder out, String name, String mode) throws IOException, InterruptedException {
        try (Running bank = Running.start(mode, "security")) {
            Browser b = bank.browser();
            out.append(name).append('\n');
            line(out, "  evil.example reads /account", b.fetch(EVIL, "GET", "/account", null, true));
            line(out, "  evilbank.example reads /account", b.fetch(LOOKALIKE, "GET", "/account", null, true));
            line(out, "  null origin reads /account", b.fetch(OPAQUE, "GET", "/account", null, true));
        }
    }

    private static void act3Exact(StringBuilder out) throws IOException, InterruptedException {
        out.append("\n== 3. An exact allow-list ==\n");
        try (Running bank = Running.start("exact", "security")) {
            Browser b = bank.browser();
            line(out, "partner.example reads /account", b.fetch(PARTNER, "GET", "/account", null, true));
            line(out, "partner.example PUTs JSON", b.fetch(PARTNER, "PUT", "/transfer", "application/json", true));
            line(out, "evil.example reads /account", b.fetch(EVIL, "GET", "/account", null, true));
            line(out, "evilbank.example reads /account", b.fetch(LOOKALIKE, "GET", "/account", null, true));
            line(out, "null origin reads /account", b.fetch(OPAQUE, "GET", "/account", null, true));
            line(out, "evil.example PUTs JSON", b.fetch(EVIL, "PUT", "/transfer", "application/json", true));
            line(out, "evil.example reads public /rates", b.fetch(EVIL, "GET", "/rates", null, false));
            line(out, "bank's own page PUTs JSON", b.fetch(bank.url(), "PUT", "/transfer", "application/json", true));
            Browser.Reply refused = b.raw("GET", "/account", EVIL, true);
            line(out, "Spring's own answer to evil.example", refused.status() + " " + refused.body() + ", before the controller ran");
            line(out, "Vary header on /account", b.raw("GET", "/account", PARTNER, true).header("vary"));
        }
    }

    private static void act4Where(StringBuilder out) throws IOException, InterruptedException {
        out.append("\n== 4. Where the policy is registered ==\n");
        for (String where : List.of("mvc", "security")) {
            try (Running bank = Running.start("exact", where)) {
                Browser b = bank.browser();
                out.append("the same exact list, registered in ").append(where.equals("mvc") ? "Spring MVC (addCorsMappings)" : "Spring Security (http.cors)").append('\n');
                line(out, "  partner.example preflight for PUT", b.preflight(PARTNER, "PUT", "/transfer", true).status());
                line(out, "  partner.example reads /account", b.fetch(PARTNER, "GET", "/account", null, true));
                line(out, "  partner.example PUTs JSON", b.fetch(PARTNER, "PUT", "/transfer", "application/json", true));
            }
        }
    }

    private static void act5Bill(StringBuilder out) throws IOException, InterruptedException {
        out.append("\n== 5. The bill ==\n");
        try (Running bank = Running.start("exact", "security")) {
            Browser b = bank.browser();
            Browser.Reply rates = b.raw("GET", "/rates", EVIL, false);
            line(out, "public /rates sends", "Allow-Origin " + rates.header("access-control-allow-origin"));
            line(out, "browser accepts * without credentials", Browser.approves(rates, EVIL, false));
            line(out, "browser accepts * with credentials", Browser.approves(rates, EVIL, true));
            int before = bank.processed().list().size();
            line(out, "evil.example POSTs /transfer (simple)", b.fetch(EVIL, "POST", "/transfer", "text/plain", true));
            Browser.Reply noOrigin = b.raw("POST", "/transfer", null, true);
            line(out, "the same POST with no Origin header", noOrigin.status() + " " + noOrigin.body() + "  <- CORS IS A BROWSER RULE, NOT A DEFENCE");
            line(out, "requests the bank processed", bank.processed().list().subList(before, bank.processed().list().size()));
        }
        out.append("every allowed origin needs a test, and so do the lookalike, the null origin and the preflight\n");
        out.append("a policy decides who may read an answer; who may act still needs a login and a token\n");
    }
}
