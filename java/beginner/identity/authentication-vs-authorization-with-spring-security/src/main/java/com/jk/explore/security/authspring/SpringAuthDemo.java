package com.jk.explore.security.authspring;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.http.HttpMethod;

/**
 * Runs the online store story in seven numbered acts, with Spring Security, and prints exact, repeatable results.
 * Each act prints what goes in (THE REQUESTS, BEFORE ...) apart from what comes out (THE RESPONSES, AFTER ...).
 */
public class SpringAuthDemo {

    /** The demo's fixed BCrypt salt, so the output repeats. Real use takes a random salt every time. */
    static final String SALT = "ShopSaltForBenAccount.";

    /** One request of an act: who sends it (null: no login at all), the password, and what it asks for. */
    record Ask(String who, String password, HttpMethod method, String path, String note, String mark) {

        static Ask as(String who, HttpMethod method, String path, String note, String mark) {
            return new Ask(who, who + "-pass", method, path, note, mark);
        }
    }

    public static void main(String[] args) {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() {
        Logger.getLogger("").setLevel(Level.WARNING);   // keep Spring's start-up chatter out of the output
        StringBuilder out = new StringBuilder();
        act1NoChecks(out);
        act2Authentication(out);
        act3BasicHeader(out);
        act4PasswordCheck(out);
        act5LoginIsNotEnough(out);
        act6Authorization(out);
        act7Bill(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    /** Prints the requests, sends them all, then prints the responses, numbered the same way. */
    private static void exchange(StringBuilder out, Shop shop, String requests, boolean counts, List<Ask> asks) {
        line(out, "  THE REQUESTS (" + requests + "):");
        for (int i = 0; i < asks.size(); i++) {
            Ask a = asks.get(i);
            line(out, String.format("    %d  %-6s %-4s %-19s %s", i + 1, a.who() == null ? "nobody" : a.who(),
                    a.method().name(), a.path(), a.note()).stripTrailing());
        }
        line(out, "  THE RESPONSES:");
        for (int i = 0; i < asks.size(); i++) {
            Ask a = asks.get(i);
            int before = shop.encoder().checks();
            Shop.Result result = shop.send(a.method(), a.path(), a.who(), a.password());
            int checks = shop.encoder().checks() - before;
            String extra = counts ? String.format("%-20s %d password check%s", result, checks, checks == 1 ? "" : "s")
                                  : result.toString();
            line(out, "    " + (i + 1) + "  " + extra + (a.mark().isEmpty() ? "" : "  <- " + a.mark()));
        }
    }

    private static void act1NoChecks(StringBuilder out) {
        line(out, "ONE. Spring Security present, everything permitted.");
        try (Shop shop = new Shop(OpenConfig.class)) {
            exchange(out, shop, "no Authorization header", false, List.of(
                    new Ask(null, null, HttpMethod.POST, "/orders/O-1/refund", "", "NOBODY ASKED WHO"),
                    new Ask(null, null, HttpMethod.GET, "/orders/O-1", "", "")));
        }
    }

    private static void act2Authentication(StringBuilder out) {
        line(out, "TWO. Authentication: HTTP Basic and BCrypt.");
        try (Shop shop = new Shop(LoginOnlyConfig.class)) {
            exchange(out, shop, "ben's own order O-2", true, List.of(
                    new Ask(null, null, HttpMethod.GET, "/orders/O-2", "no login", ""),
                    new Ask("ben", "guess", HttpMethod.GET, "/orders/O-2", "password guess", ""),
                    Ask.as("ben", HttpMethod.GET, "/orders/O-2", "password ben-pass", "")));
        }
    }

    private static void act3BasicHeader(StringBuilder out) {
        line(out, "THREE. The login on the wire: the HTTP Basic header.");
        String joined = BasicHeader.joined("ben", "ben-pass");
        line(out, "  BEFORE ENCODING (what ben types):");
        line(out, "    username ben, password ben-pass");
        line(out, "    joined   " + joined + "   (" + joined.length() + " bytes)");
        line(out, "  STEP BY STEP (every 3 bytes become 4 letters):");
        for (String[] step : BasicHeader.steps(joined)) {
            line(out, "    " + step[0] + "  ->  " + step[1]);
        }
        String header = BasicHeader.header("ben", "ben-pass");
        line(out, "  AFTER ENCODING (what every request carries):");
        line(out, "    Authorization: " + header);
        line(out, "  anyone can decode it: " + BasicHeader.decode(header.substring(6))
                + "  <- NOT ENCRYPTED");
    }

    private static void act4PasswordCheck(StringBuilder out) {
        line(out, "FOUR. The password check, step by step: BCrypt.");
        HashSteps.Parts stored = HashSteps.hash("ben-pass", SALT, 4);
        line(out, "  BEFORE HASHING (when ben signs up):");
        line(out, "    password   ben-pass");
        line(out, "    salt       " + SALT);
        line(out, "    cost       4, so 2^4 = 16 rounds");
        line(out, "  AFTER HASHING (the stored line, in four parts):");
        line(out, "    version    " + stored.version());
        line(out, "    cost       " + stored.cost());
        line(out, "    salt       " + stored.salt());
        line(out, "    hash       " + stored.hash());
        line(out, "  CHECKING A LOGIN (hash again, same salt):");
        CountingPasswordEncoder encoder = new CountingPasswordEncoder();
        for (String typed : List.of("ben-pass", "guess")) {
            HashSteps.Parts again = HashSteps.again(typed, stored);
            line(out, String.format("    %-9s %s  %s", typed, again.hash(), again.hash().equals(stored.hash()) ? "same" : "different"));
        }
        line(out, "  Spring's encoder agrees: " + encoder.matches("ben-pass", stored.stored())
                + ", " + encoder.matches("guess", stored.stored()));
    }

    private static void act5LoginIsNotEnough(StringBuilder out) {
        line(out, "FIVE. Authenticated is not the same as authorized.");
        try (Shop shop = new Shop(LoginOnlyConfig.class)) {
            exchange(out, shop, "ben, a customer, logged in", false, List.of(
                    Ask.as("ben", HttpMethod.POST, "/orders/O-1/refund", "", "LOGGED IN, SO ALLOWED"),
                    Ask.as("ben", HttpMethod.GET, "/orders/O-1", "ana's order", "NOT HIS ORDER")));
        }
    }

    private static void act6Authorization(StringBuilder out) {
        line(out, "SIX. Authorization: @PreAuthorize rules on the endpoints.");
        try (Shop shop = new Shop(PolicyConfig.class)) {
            exchange(out, shop, "the same shop, now with rules", false, List.of(
                    new Ask(null, null, HttpMethod.POST, "/orders/O-1/refund", "", ""),
                    Ask.as("ben", HttpMethod.POST, "/orders/O-1/refund", "customer", ""),
                    Ask.as("ben", HttpMethod.GET, "/orders/O-1", "ana's order", ""),
                    Ask.as("ben", HttpMethod.GET, "/orders/O-2", "his own", ""),
                    Ask.as("sam", HttpMethod.GET, "/orders/O-1", "support", ""),
                    Ask.as("sam", HttpMethod.POST, "/orders/O-1/refund", "support", ""),
                    Ask.as("ana", HttpMethod.POST, "/orders/O-2/cancel", "ben's order", ""),
                    Ask.as("alex", HttpMethod.POST, "/orders/O-2/cancel", "admin", "")));
        }
    }

    private static void act7Bill(StringBuilder out) {
        line(out, "SEVEN. The bill: a forgotten rule, a check every time.");
        try (Shop shop = new Shop(PolicyConfig.class)) {
            int before = shop.encoder().checks();
            shop.as("sam", HttpMethod.GET, "/orders/O-1");
            shop.as("sam", HttpMethod.GET, "/orders/O-2");
            shop.as("sam", HttpMethod.POST, "/orders/O-1/refund");
            line(out, "  EVERY REQUEST PAYS (the password goes every time):");
            line(out, "    sam sends 3 requests: " + (shop.encoder().checks() - before) + " password checks");
            line(out, "  BEFORE THE FIX (export has no @PreAuthorize):");
            line(out, "    ben   GET /orders/export: " + shop.as("ben", HttpMethod.GET, "/orders/export")
                    + "  <- FORGOTTEN RULE");
        }
        try (Shop shop = new Shop(LayeredConfig.class)) {
            line(out, "  AFTER THE FIX (/orders/export needs ADMIN):");
            line(out, "    ben   GET /orders/export: " + shop.as("ben", HttpMethod.GET, "/orders/export"));
            line(out, "    alex  GET /orders/export: " + shop.as("alex", HttpMethod.GET, "/orders/export"));
        }
    }
}
