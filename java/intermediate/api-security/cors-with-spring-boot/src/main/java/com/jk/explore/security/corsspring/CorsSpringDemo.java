package com.jk.explore.security.corsspring;

import java.io.IOException;
import java.util.List;
import org.springframework.web.cors.CorsConfiguration;

/**
 * Runs the bank API story in five numbered acts against real Spring Boot servers on the loopback interface. Each act prints the
 * policy or the request, the headers the bank sends back and what the page gets, in separate labelled sections. Ports are never
 * printed, so the output is the same on every run.
 */
public class CorsSpringDemo {

    static final String PARTNER = "https://partner.example";
    static final String EVIL = "https://evil.example";
    static final String LOOKALIKE = "https://evilbank.example";
    static final String OPAQUE = "null";

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() throws IOException, InterruptedException {
        StringBuilder out = new StringBuilder();
        act1NoPolicy(out);
        act2LooksLikeFixes(out);
        act3Exact(out);
        act4Where(out);
        act5Bill(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void act1NoPolicy(StringBuilder out) throws IOException, InterruptedException {
        line(out, "ONE. With no policy, a page may send but not read.");
        try (Running bank = Running.start("none", "security")) {
            Browser b = bank.browser();
            line(out, "  THE REQUESTS (a script on " + EVIL + ", with login):");
            line(out, "    GET  /account                     " + simple("GET", null));
            line(out, "    POST /transfer  text/plain        " + simple("POST", "text/plain"));
            line(out, "    PUT  /transfer  application/json  " + simple("PUT", "application/json"));
            line(out, "  THE PREFLIGHT ANSWER (OPTIONS /transfer, asking for PUT):");
            Browser.Reply pre = b.preflight(EVIL, "PUT", "/transfer", true);
            line(out, "    " + pre.status() + ", Access-Control-Allow-Origin: " + orNone(pre.header("access-control-allow-origin")));
            int before = bank.processed().list().size();
            line(out, "  WHAT THE PAGE GETS:");
            line(out, "    GET  /account   " + got(b.fetch(EVIL, "GET", "/account", null, true)));
            line(out, "    POST /transfer  " + got(b.fetch(EVIL, "POST", "/transfer", "text/plain", true)));
            line(out, "    PUT  /transfer  " + got(b.fetch(EVIL, "PUT", "/transfer", "application/json", true)));
            line(out, "  THE BANK'S LOG (the requests it ran):");
            line(out, "    " + since(bank, before) + "  <- CORS DID NOT STOP THE POST");
            line(out, "  the bank's own page: " + got(b.fetch(bank.url(), "GET", "/account", null, true)));
        }
    }

    private static void act2LooksLikeFixes(StringBuilder out) throws IOException, InterruptedException {
        line(out, "TWO. Four policies that look like fixes.");
        describe(out, new CorsSettings("star-with-credentials", "mvc").accounts());
        try (Running bank = Running.start("star-with-credentials", "mvc")) {
            Browser.Reply r = bank.browser().raw("GET", "/account", EVIL, true);
            line(out, "  THE ANSWER TO " + EVIL + ":");
            line(out, "    " + r.status() + ", Access-Control-Allow-Origin: " + orNone(r.header("access-control-allow-origin")));
            line(out, "  Spring refuses * with credentials, at the first request");
        }
        policy(out, "reflect");
        policy(out, "exact-and-null");
        policy(out, "suffix");
        CorsConfiguration suffix = new CorsSettings("suffix", "security").accounts();
        line(out, "  HOW THE PATTERN DECIDES (* may be any text, even \"evil\"):");
        for (String site : List.of(PARTNER, EVIL, LOOKALIKE)) {
            line(out, String.format("    %-26s%s", site, suffix.checkOrigin(site) != null ? "matches" : "no match"));
        }
        line(out, "  each page that reads gets: balance: 125000 cents");
    }

    /** One policy: what it says, then each origin sent, the Access-Control-Allow-Origin that comes back, and the result. */
    private static void policy(StringBuilder out, String mode) throws IOException, InterruptedException {
        describe(out, new CorsSettings(mode, "security").accounts());
        try (Running bank = Running.start(mode, "security")) {
            Browser b = bank.browser();
            line(out, "  THE ANSWERS:");
            line(out, String.format("    %-26s%-26s%s", "origin sent", "Allow-Origin back", "answer"));
            for (String site : List.of(EVIL, LOOKALIKE, OPAQUE)) {
                Browser.Reply r = b.raw("GET", "/account", site, true);
                FetchResult f = b.fetch(site, "GET", "/account", null, true);
                line(out, String.format("    %-26s%-26s%d %s", site, orNone(r.header("access-control-allow-origin")), r.status(),
                        f.readable() ? "reads" : "blocked"));
            }
        }
    }

    private static void act3Exact(StringBuilder out) throws IOException, InterruptedException {
        line(out, "THREE. An exact allow-list names one partner.");
        CorsConfiguration c = new CorsSettings("exact", "security").accounts();
        line(out, "  THE POLICY (CorsSettings.accounts):");
        line(out, "    allowedOrigins " + c.getAllowedOrigins());
        line(out, "    allowedMethods " + c.getAllowedMethods());
        line(out, "    allowedHeaders " + c.getAllowedHeaders());
        line(out, "    allowCredentials " + c.getAllowCredentials() + ", maxAge " + c.getMaxAge());
        try (Running bank = Running.start("exact", "security")) {
            Browser b = bank.browser();
            Browser.Reply pre = b.preflight(PARTNER, "PUT", "/transfer", true);
            line(out, "  THE PREFLIGHT ANSWER (the partner asks before a JSON PUT):");
            line(out, "    " + pre.status());
            headers(out, pre);
            line(out, "  WHAT EACH PAGE GETS:");
            line(out, row("partner.example", "GET /account", b.fetch(PARTNER, "GET", "/account", null, true)));
            line(out, row("partner.example", "PUT /transfer", b.fetch(PARTNER, "PUT", "/transfer", "application/json", true)));
            line(out, row("evil.example", "GET /account", b.fetch(EVIL, "GET", "/account", null, true)));
            line(out, row("evilbank.example", "GET /account", b.fetch(LOOKALIKE, "GET", "/account", null, true)));
            line(out, row("null", "GET /account", b.fetch(OPAQUE, "GET", "/account", null, true)));
            line(out, row("evil.example", "PUT /transfer", b.fetch(EVIL, "PUT", "/transfer", "application/json", true)));
            line(out, row("evil.example", "GET /rates", b.fetch(EVIL, "GET", "/rates", null, false)) + "  (public: *)");
            line(out, row("own page", "PUT /transfer", b.fetch(bank.url(), "PUT", "/transfer", "application/json", true)));
            int before = bank.processed().list().size();
            Browser.Reply refused = b.raw("GET", "/account", EVIL, true);
            line(out, "  SPRING'S OWN ANSWER TO " + EVIL + ":");
            line(out, "    " + refused.status() + " " + refused.body() + "; the bank's log grew by " + (bank.processed().list().size() - before));
            line(out, "  THE Vary HEADERS ON /account (asked by the partner):");
            for (String vary : b.raw("GET", "/account", PARTNER, true).raw().headers().allValues("vary")) {
                line(out, "    Vary: " + vary);
            }
        }
    }

    private static void act4Where(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FOUR. The policy must sit before the login check.");
        line(out, "  THE ORDER A REQUEST GOES THROUGH:");
        line(out, "    1. Spring Security: http.cors, then the login check");
        line(out, "    2. Spring MVC: addCorsMappings, then the controller");
        for (String where : List.of("mvc", "security")) {
            try (Running bank = Running.start("exact", where)) {
                Browser b = bank.browser();
                line(out, where.equals("mvc") ? "  REGISTERED IN SPRING MVC (addCorsMappings):" : "  REGISTERED IN SPRING SECURITY (http.cors):");
                line(out, "    preflight for PUT (no login)  " + b.preflight(PARTNER, "PUT", "/transfer", true).status());
                line(out, "    GET  /account                 " + got(b.fetch(PARTNER, "GET", "/account", null, true)));
                line(out, "    PUT  /transfer                " + got(b.fetch(PARTNER, "PUT", "/transfer", "application/json", true)));
            }
        }
        line(out, "  a preflight never carries a login, so it must pass before one");
    }

    private static void act5Bill(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FIVE. CORS is a browser rule, not a defence.");
        try (Running bank = Running.start("exact", "security")) {
            Browser b = bank.browser();
            Browser.Reply rates = b.raw("GET", "/rates", EVIL, false);
            line(out, "  THE PUBLIC ANSWER (GET /rates):");
            line(out, "    Access-Control-Allow-Origin: " + rates.header("access-control-allow-origin"));
            line(out, "  THE BROWSER'S CHECK OF *:");
            line(out, "    without credentials  -> " + (Browser.approves(rates, EVIL, false) ? "accepted" : "refused"));
            line(out, "    with credentials     -> " + (Browser.approves(rates, EVIL, true) ? "accepted" : "refused"));
            int before = bank.processed().list().size();
            line(out, "  A POST /transfer FROM A PAGE ON " + EVIL + ":");
            line(out, "    " + got(b.fetch(EVIL, "POST", "/transfer", "text/plain", true)));
            Browser.Reply noOrigin = b.raw("POST", "/transfer", null, true);
            line(out, "  THE SAME POST FROM A SERVER SCRIPT (no Origin header):");
            line(out, "    " + noOrigin.status() + " " + noOrigin.body() + "  <- NO BROWSER, NO CORS");
            line(out, "  THE BANK'S LOG (the requests it ran):");
            line(out, "    " + since(bank, before));
        }
        line(out, "  who may act still needs a login and a CSRF token");
    }

    /** A policy as Spring holds it: whether credentials are allowed, and the origin list or the origin patterns. */
    private static void describe(StringBuilder out, CorsConfiguration c) {
        line(out, "  THE POLICY (allowCredentials " + c.getAllowCredentials() + "):");
        line(out, "    " + (c.getAllowedOriginPatterns() != null ? "allowedOriginPatterns " + c.getAllowedOriginPatterns()
                : "allowedOrigins " + c.getAllowedOrigins()));
    }

    /** The CORS headers of a reply, with their usual capitalisation, in a fixed order. */
    private static void headers(StringBuilder out, Browser.Reply r) {
        for (String name : List.of("Access-Control-Allow-Origin", "Access-Control-Allow-Methods", "Access-Control-Allow-Headers",
                "Access-Control-Allow-Credentials", "Access-Control-Max-Age")) {
            String value = r.header(name);
            if (value != null) {
                line(out, "    " + name + ": " + value);
            }
        }
    }

    private static List<String> since(Running bank, int before) {
        List<String> all = bank.processed().list();
        return all.subList(before, all.size());
    }

    private static String simple(String method, String contentType) {
        return Browser.isSimple(method, contentType) ? "simple" : "needs a preflight";
    }

    private static String row(String site, String call, FetchResult r) {
        return String.format("    %-17s%-15s%s", site, call, got(r));
    }

    private static String got(FetchResult r) {
        return r.readable() ? "read: " + r.body() : r.reachedBank() ? "reached the bank, answer blocked" : "never reached the bank";
    }

    private static String orNone(String value) {
        return value == null ? "(none)" : value;
    }
}
