package com.jk.explore.security.cors;

import java.util.Set;

/** Runs the bank API story in five numbered acts and prints exact, repeatable results. */
public class CorsDemo {

    static final String PARTNER = "https://partner.example";
    static final String EVIL = "https://evil.example";
    static final String LOOKALIKE = "https://evilbank.example";
    static final String OPAQUE = "null";
    static final Set<String> PARTNERS = Set.of(PARTNER);

    public static void main(String[] args) {
        System.out.print(run());
    }

    public static String run() {
        StringBuilder out = new StringBuilder();

        out.append("== 1. Same origin needs no CORS ==\n");
        Browser b = new Browser(new BankApi(CorsPolicy.NONE, false));
        out.append(line("bank's own page reads /account", b.fetch(BankApi.ORIGIN, "GET", "/account", null, true)));

        out.append("\n== 2. No CORS headers: a page may send, but not read ==\n");
        BankApi plain = new BankApi(CorsPolicy.NONE, false);
        Browser pb = new Browser(plain);
        out.append(line("evil.example reads /account", pb.fetch(EVIL, "GET", "/account", null, true)));
        out.append(line("evil.example POSTs /transfer (simple)", pb.fetch(EVIL, "POST", "/transfer", "text/plain", true)));
        out.append(line("evil.example PUTs /transfer", pb.fetch(EVIL, "PUT", "/transfer", "application/json", true)));
        out.append("requests the bank processed -> ").append(plain.processed()).append("  <- CORS DID NOT STOP THE POST\n");

        out.append("\n== 3. Policies that look like fixes ==\n");
        out.append(policyLines("reflect any origin", CorsPolicy.REFLECT_ANY));
        out.append(policyLines("exact list that also allows null", CorsPolicy.exactAndNull(PARTNERS)));
        out.append(policyLines("origin ends with bank.example", CorsPolicy.suffix("bank.example")));

        out.append("\n== 4. An exact allow-list ==\n");
        BankApi exact = new BankApi(CorsPolicy.exact(PARTNERS), true);
        Browser eb = new Browser(exact);
        out.append(line("partner.example reads /account", eb.fetch(PARTNER, "GET", "/account", null, true)));
        out.append(line("partner.example PUTs JSON", eb.fetch(PARTNER, "PUT", "/transfer", "application/json", true)));
        out.append(line("evil.example reads /account", eb.fetch(EVIL, "GET", "/account", null, true)));
        out.append(line("evilbank.example reads /account", eb.fetch(LOOKALIKE, "GET", "/account", null, true)));
        out.append(line("null origin reads /account", eb.fetch(OPAQUE, "GET", "/account", null, true)));
        out.append(line("evil.example PUTs JSON", eb.fetch(EVIL, "PUT", "/transfer", "application/json", true)));
        out.append(line("evil.example reads public /rates", eb.fetch(EVIL, "GET", "/rates", null, false)));
        out.append("Vary header on /account       -> ").append(exact.handle(new Request("GET", "/account", java.util.Map.of("origin", PARTNER), true)).header("vary")).append('\n');

        out.append("\n== 5. Wildcard and cookies never combine ==\n");
        Response star = Response.of(200, "balance").with("access-control-allow-origin", "*").with("access-control-allow-credentials", "true");
        out.append("browser accepts * with cookies -> ").append(Browser.approves(star, EVIL, true)).append('\n');
        out.append("browser accepts * without cookies -> ").append(Browser.approves(star, EVIL, false)).append('\n');
        return out.toString();
    }

    private static String policyLines(String name, CorsPolicy policy) {
        Browser b = new Browser(new BankApi(policy, false));
        return String.format("%s%n%s%s%s", name,
                line("  evil.example reads /account", b.fetch(EVIL, "GET", "/account", null, true)),
                line("  evilbank.example reads /account", b.fetch(LOOKALIKE, "GET", "/account", null, true)),
                line("  null origin reads /account", b.fetch(OPAQUE, "GET", "/account", null, true)));
    }

    private static String line(String label, FetchResult r) {
        return String.format("%-38s -> %s%n", label, r);
    }
}
