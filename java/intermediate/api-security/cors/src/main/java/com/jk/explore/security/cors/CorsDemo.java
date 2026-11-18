package com.jk.explore.security.cors;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Runs the bank API story in five numbered acts and prints exact, repeatable results. Each act prints the request, the
 * headers the bank sends back and what the page gets, in separate labelled sections.
 */
public class CorsDemo {

    static final String PARTNER = "https://partner.example";
    static final String EVIL = "https://evil.example";
    static final String LOOKALIKE = "https://evilbank.example";
    static final String OPAQUE = "null";
    static final Set<String> PARTNERS = Set.of(PARTNER);
    static final String SUFFIX = "bank.example";

    public static void main(String[] args) {
        System.out.print(run());
    }

    public static String run() {
        StringBuilder out = new StringBuilder();

        line(out, "ONE. The bank's own page needs no CORS.");
        line(out, "  THE ORIGINS (scheme, host, port; same as the API?):");
        Origin api = Origin.parse(BankApi.ORIGIN);
        for (String site : List.of(BankApi.ORIGIN, PARTNER, EVIL, LOOKALIKE)) {
            Origin o = Origin.parse(site);
            line(out, String.format("    %-26s%-7s%-18s%-5d%s", site, o.scheme(), o.host(), o.port(), o.sameAs(api) ? "yes" : "no"));
        }
        line(out, "  THE REQUEST (a script on " + BankApi.ORIGIN + ", with cookie):");
        line(out, "    GET /account");
        line(out, "  WHAT THE PAGE GETS (same origin: no CORS rules apply):");
        Browser own = new Browser(new BankApi(CorsPolicy.NONE, false));
        line(out, "    " + got(own.fetch(BankApi.ORIGIN, "GET", "/account", null, true)));

        line(out, "TWO. With no CORS headers, a page may send but not read.");
        line(out, "  THE REQUESTS (a script on " + EVIL + ", cookie sent):");
        line(out, "    GET  /account                     " + simple("GET", null));
        line(out, "    POST /transfer  text/plain        " + simple("POST", "text/plain"));
        line(out, "    PUT  /transfer  application/json  " + simple("PUT", "application/json"));
        line(out, "  THE SIMPLE-REQUEST RULE (sent without asking first):");
        line(out, "    GET, HEAD or POST, with a form or plain-text body");
        BankApi plain = new BankApi(CorsPolicy.NONE, false);
        line(out, "  THE PREFLIGHT (asked before the PUT, without the cookie):");
        line(out, "    OPTIONS /transfer");
        line(out, "    Access-Control-Request-Method: PUT");
        line(out, "    Access-Control-Request-Headers: content-type");
        line(out, "  THE PREFLIGHT ANSWER:");
        Response pre = plain.handle(preflight(EVIL));
        line(out, "    " + pre.status() + ", Access-Control-Allow-Origin: " + orNone(pre.header("access-control-allow-origin")));
        Browser pb = new Browser(plain);
        line(out, "  WHAT THE PAGE GETS:");
        line(out, "    GET  /account   " + got(pb.fetch(EVIL, "GET", "/account", null, true)));
        line(out, "    POST /transfer  " + got(pb.fetch(EVIL, "POST", "/transfer", "text/plain", true)));
        line(out, "    PUT  /transfer  " + got(pb.fetch(EVIL, "PUT", "/transfer", "application/json", true)));
        line(out, "  THE BANK'S LOG (the requests it ran):");
        line(out, "    " + plain.processed() + "  <- CORS DID NOT STOP THE POST");

        line(out, "THREE. Three policies that look like fixes.");
        policy(out, "reflect any origin, with credentials", CorsPolicy.REFLECT_ANY);
        policy(out, "the partner, and also null", CorsPolicy.exactAndNull(PARTNERS));
        policy(out, "the origin ends with " + SUFFIX, CorsPolicy.suffix(SUFFIX));
        line(out, "  HOW THE SUFFIX TEST DECIDES:");
        for (String site : List.of(PARTNER, EVIL, LOOKALIKE)) {
            line(out, "    \"" + site + "\".endsWith(\"" + SUFFIX + "\") -> " + site.endsWith(SUFFIX));
        }
        line(out, "  each page that reads gets: balance: 125000 cents");

        line(out, "FOUR. An exact allow-list names one partner.");
        BankApi exact = new BankApi(CorsPolicy.exact(PARTNERS), true);
        line(out, "  THE LIST (whole strings, compared case-sensitively):");
        for (String site : PARTNERS) {
            line(out, "    " + site);
        }
        line(out, "  THE PREFLIGHT (the partner asks before a JSON PUT):");
        line(out, "    OPTIONS /transfer   Origin: " + PARTNER);
        line(out, "  THE PREFLIGHT ANSWER:");
        headers(out, exact.handle(preflight(PARTNER)));
        Response account = exact.handle(new Request("GET", "/account", Map.of("origin", PARTNER), true));
        line(out, "  THE ANSWER TO GET /account (the partner, with cookie):");
        headers(out, account);
        line(out, "  THE BROWSER'S CHECK (a request with cookies):");
        String allow = account.header("access-control-allow-origin");
        line(out, "    Allow-Origin equals the page's origin  -> " + yes(allow.equals(PARTNER)));
        line(out, "    Allow-Credentials is true              -> " + yes("true".equals(account.header("access-control-allow-credentials"))));
        line(out, "    so the page may read                   -> " + yes(Browser.approves(account, PARTNER, true)));
        line(out, "  WHAT EACH PAGE GETS:");
        Browser eb = new Browser(exact);
        line(out, row("partner.example", "GET /account", eb.fetch(PARTNER, "GET", "/account", null, true)));
        line(out, row("partner.example", "PUT /transfer", eb.fetch(PARTNER, "PUT", "/transfer", "application/json", true)));
        line(out, row("evil.example", "GET /account", eb.fetch(EVIL, "GET", "/account", null, true)));
        line(out, row("evilbank.example", "GET /account", eb.fetch(LOOKALIKE, "GET", "/account", null, true)));
        line(out, row("null", "GET /account", eb.fetch(OPAQUE, "GET", "/account", null, true)));
        line(out, row("evil.example", "PUT /transfer", eb.fetch(EVIL, "PUT", "/transfer", "application/json", true)));
        line(out, row("evil.example", "GET /rates", eb.fetch(EVIL, "GET", "/rates", null, false)) + "  (public: *)");

        line(out, "FIVE. A wildcard and cookies never combine.");
        Response star = Response.of(200, "balance").with("access-control-allow-origin", "*").with("access-control-allow-credentials", "true");
        line(out, "  THE ANSWER (the server sends both):");
        headers(out, star);
        line(out, "  THE BROWSER'S CHECK, A REQUEST WITH COOKIES:");
        line(out, "    Allow-Origin must equal the page's origin; * does not");
        line(out, "    -> " + (Browser.approves(star, EVIL, true) ? "accepted" : "refused"));
        line(out, "  THE BROWSER'S CHECK, A REQUEST WITHOUT COOKIES:");
        line(out, "    * means any page may read");
        line(out, "    -> " + (Browser.approves(star, EVIL, false) ? "accepted" : "refused"));
        line(out, "  needing cookies and any origin leads to reflecting (THREE)");
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    /** One unsafe policy: each origin sent, the Access-Control-Allow-Origin the bank sends back, and whether the page reads. */
    private static void policy(StringBuilder out, String name, CorsPolicy policy) {
        line(out, "  THE POLICY: " + name);
        line(out, "  THE ANSWERS:");
        line(out, String.format("    %-26s%-26s%s", "origin sent", "Allow-Origin back", "page"));
        Browser b = new Browser(new BankApi(policy, false));
        for (String site : List.of(EVIL, LOOKALIKE, OPAQUE)) {
            FetchResult r = b.fetch(site, "GET", "/account", null, true);
            line(out, String.format("    %-26s%-26s%s", site, orNone(policy.allow(site).orElse(null)), r.readable() ? "reads" : "blocked"));
        }
    }

    private static Request preflight(String origin) {
        return new Request("OPTIONS", "/transfer",
                Map.of("origin", origin, "access-control-request-method", "PUT", "access-control-request-headers", "content-type"), false);
    }

    /** The CORS headers of a response, with their usual capitalisation, in a fixed order. */
    private static void headers(StringBuilder out, Response r) {
        for (String name : List.of("Access-Control-Allow-Origin", "Access-Control-Allow-Credentials", "Access-Control-Allow-Methods",
                "Access-Control-Allow-Headers", "Access-Control-Max-Age", "Vary")) {
            String value = r.header(name);
            if (value != null) {
                line(out, "    " + name + ": " + value);
            }
        }
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

    private static String yes(boolean b) {
        return b ? "yes" : "no";
    }
}
