package com.jk.explore.security.ssrf;

import java.net.InetAddress;
import java.util.List;

/**
 * Runs the product image import story in five numbered acts and prints exact, repeatable results. Each act prints the requests, how the
 * server reached its answer, and the responses, in separate labelled sections.
 */
public class SsrfDemo {

    static final String HONEST = "http://images.example/cat.png";
    static final String METADATA = "http://169.254.169.254" + Network.CREDENTIALS_PATH;
    static final String LOCALHOST = "http://localhost/actuator/env";
    static final String INTERNAL = "http://10.0.0.5/admin";
    static final String DECIMAL = "http://2852039166" + Network.CREDENTIALS_PATH;
    static final String SHORT_LOOPBACK = "http://127.1/actuator/env";
    static final String MAPPED = "http://[::ffff:a9fe:a9fe]" + Network.CREDENTIALS_PATH;
    static final String DNS_NAME = "http://intranet.attacker.example/admin";
    static final String REDIRECT = "http://attacker.example/go";
    static final String REBINDING = "http://rebind.attacker.example/admin";
    static final String USER_INFO = "http://images.example@10.0.0.5/admin";
    static final String DUAL = "http://dual.attacker.example/admin";

    /** The network the store's server can reach: address or name, and what answers there. */
    private static final String[][] HOSTS = {
            {"images.example", "a public image host"}, {"attacker.example", "the attacker's host"},
            {"169.254.169.254", "cloud metadata service"}, {"localhost", "the server itself"}, {"10.0.0.5", "office admin page"}};

    /** What a check-then-connect importer saw: the answer it checked, the answer it connected to, and what it fetched. */
    record CheckThenConnect(String checked, boolean looksPublic, String connected, String fetched) {
    }

    public static void main(String[] args) {
        System.out.print(run());
    }

    public static String run() {
        StringBuilder out = new StringBuilder();
        Network net = Network.world();

        line(out, "ONE. An honest image import works.");
        line(out, "  THE NETWORK THE SERVER CAN REACH (address, name, what):");
        Dns dns = Network.dns();
        for (String[] h : HOSTS) {
            String address = dns.resolve(h[0]).get(0).getHostAddress();
            String name = address.equals(h[0]) ? "(no name)" : h[0];
            line(out, String.format("    %-16s %-17s %s", address, name, h[1]));
        }
        line(out, "  THE REQUEST (the address the seller pastes):");
        line(out, "    " + HONEST);
        Url honest = Url.parse(HONEST);
        InetAddress image = dns.resolve(honest.host()).get(0);
        line(out, "  HOW THE SERVER FETCHES IT:");
        line(out, "    look up " + honest.host() + ": " + image.getHostAddress());
        line(out, "    GET " + honest.path() + " from " + image.getHostAddress());
        line(out, "  THE RESPONSE:");
        line(out, "    " + attempt(new NaiveImporter(Network.dns(), net), HONEST));

        line(out, "TWO. The server fetches whatever it is told.");
        Importer naive = new NaiveImporter(Network.dns(), net);
        requests(out, "THE REQUESTS (addresses the seller pastes):", METADATA, LOCALHOST, INTERNAL);
        line(out, "  THE RESPONSES (what the server imported and showed):");
        line(out, "    a  " + naive.importImage(METADATA) + "  <- THE CLOUD KEY");
        line(out, "    b  " + naive.importImage(LOCALHOST) + "  <- A DATABASE PASSWORD");
        line(out, "    c  " + naive.importImage(INTERNAL) + "  <- AN ADMIN PAGE");

        line(out, "THREE. A blocklist of names is easy to walk around.");
        line(out, "  THE BLOCKLIST (compared with the host, as text):");
        line(out, "    " + String.join(", ", BlocklistImporter.BLOCKED) + ", "
                + String.join(", ", BlocklistImporter.BLOCKED_STARTS.stream().map(s -> s + "*").toList()));
        List<String> walkArounds = List.of(METADATA, DECIMAL, SHORT_LOOPBACK, MAPPED, DNS_NAME, REDIRECT);
        requests(out, "THE REQUESTS:", walkArounds.toArray(String[]::new));
        responses(out, new BlocklistImporter(Network.dns(), net), walkArounds);
        worked(out, "b", "2852039166", Spelling.decimal(2852039166L));
        worked(out, "c", "127.1", Spelling.shortForm("127.1"));
        worked(out, "d", "::ffff:a9fe:a9fe", Spelling.mapped("::ffff:a9fe:a9fe"));
        line(out, "  HOW e AND f GET INSIDE:");
        line(out, "    look up intranet.attacker.example: " + address(dns, "intranet.attacker.example"));
        Url go = Url.parse(REDIRECT);
        Response redirect = net.get(dns.resolve(go.host()).get(0), go.path());
        line(out, "    GET " + go.path() + " at " + go.host() + ": " + redirect.status() + ", go to "
                + Url.parse(redirect.location()).host());

        line(out, "FOUR. Check the address the connection really uses.");
        SafeImporter safe = new SafeImporter(Network.dns(), net);
        line(out, "  THE REQUEST (f, the redirect, again):");
        line(out, "    " + REDIRECT);
        String result = attempt(safe, REDIRECT);
        line(out, "  THE CHECKS, AS THE IMPORTER LOGS THEM:");
        trace(out, safe);
        line(out, "  THE RESPONSE:");
        line(out, "    " + result);
        List<String> attacks = List.of(HONEST, METADATA, LOCALHOST, DECIMAL, SHORT_LOOPBACK, MAPPED, DNS_NAME, USER_INFO, DUAL);
        requests(out, "THE REQUESTS:", attacks.toArray(String[]::new));
        responses(out, safe, attacks);
        attempt(safe, DUAL);
        line(out, "  ONE NAME, TWO ANSWERS (request i):");
        line(out, "    " + safe.trace().get(1));
        line(out, "    " + safe.trace().get(2));

        line(out, "FIVE. A second lookup can give a different answer.");
        line(out, "  THE REQUEST:");
        line(out, "    " + REBINDING);
        CheckThenConnect twice = checkThenFetch(Network.dns(), net);
        line(out, "  CHECK, THEN CONNECT (the name is looked up twice):");
        line(out, "    lookup 1, for the check: " + twice.checked() + (twice.looksPublic() ? ", public" : ", not public"));
        line(out, "    lookup 2, to connect:    " + twice.connected());
        line(out, "  THE RESPONSE:");
        line(out, "    " + twice.fetched() + "  <- NOT CHECKED");
        SafeImporter once = new SafeImporter(Network.dns(), net);
        String onceResult = attempt(once, REBINDING);
        line(out, "  THE SAFE IMPORTER (looks the name up once):");
        trace(out, once);
        line(out, "  THE RESPONSE:");
        line(out, "    " + onceResult + "  <- NOTHING INSIDE");
        return out.toString();
    }

    /** A "check, then connect" importer that resolves the name twice: the second answer is not the one it checked. */
    static CheckThenConnect checkThenFetch(Dns dns, Network net) {
        Url url = Url.parse(REBINDING);
        InetAddress checked = dns.resolve(url.host()).get(0);
        boolean looksPublic = AddressPolicy.isPublic(checked);
        InetAddress connected = dns.resolve(url.host()).get(0); // the HTTP client looks the name up again
        String fetched = net.get(connected, url.path()).body();
        return new CheckThenConnect(checked.getHostAddress(), looksPublic, connected.getHostAddress(), fetched);
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void requests(StringBuilder out, String header, String... addresses) {
        line(out, "  " + header);
        for (int i = 0; i < addresses.length; i++) {
            line(out, "    " + (char) ('a' + i) + "  " + addresses[i]);
        }
    }

    private static void responses(StringBuilder out, Importer importer, List<String> addresses) {
        line(out, "  THE RESPONSES:");
        for (int i = 0; i < addresses.size(); i++) {
            line(out, "    " + (char) ('a' + i) + "  " + attempt(importer, addresses.get(i)));
        }
    }

    /** The steps that turn an unusual spelling into the address, and Java's own reading of it for comparison. */
    private static void worked(StringBuilder out, String key, String spelling, Spelling.Worked w) {
        line(out, "  HOW " + key + ", " + spelling + ", BECOMES AN ADDRESS:");
        for (String step : w.steps()) {
            line(out, "    " + step);
        }
        String java = address(Network.dns(), spelling);
        line(out, "    -> " + w.address() + (java.equals(w.address()) ? " (Java reads it the same)" : " (Java reads " + java + ")"));
    }

    private static void trace(StringBuilder out, SafeImporter importer) {
        for (String t : importer.trace()) {
            line(out, "    " + t);
        }
    }

    private static String address(Dns dns, String host) {
        return dns.resolve(host).get(0).getHostAddress();
    }

    private static String attempt(Importer importer, String address) {
        try {
            return "imported: " + importer.importImage(address);
        } catch (ImportRefused e) {
            return "refused: " + e.getMessage();
        } catch (Unreachable e) {
            return "failed: " + e.getMessage();
        }
    }

    static List<String> attacks() {
        return List.of(METADATA, LOCALHOST, INTERNAL, DECIMAL, SHORT_LOOPBACK, MAPPED, DNS_NAME, REDIRECT, REBINDING, USER_INFO, DUAL);
    }
}
