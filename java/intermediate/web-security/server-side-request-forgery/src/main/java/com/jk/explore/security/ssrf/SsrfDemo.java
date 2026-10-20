package com.jk.explore.security.ssrf;

import java.util.List;

/** Runs the product image import story in four numbered acts and prints exact, repeatable results. */
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
    static final String REBINDING = "http://rebind.attacker.example/cat.png";
    static final String USER_INFO = "http://images.example@10.0.0.5/admin";

    public static void main(String[] args) {
        System.out.print(run());
    }

    public static String run() {
        StringBuilder out = new StringBuilder();
        Dns dns = Network.dns();
        Network net = Network.world();

        out.append("== 1. Importing an image from a web address ==\n");
        line(out, "honest address", new NaiveImporter(dns, net), HONEST);

        out.append("\n== 2. The server fetches whatever it is told ==\n");
        Importer naive = new NaiveImporter(dns, net);
        line(out, "metadata address", naive, METADATA);
        line(out, "localhost", naive, LOCALHOST);
        line(out, "office admin page", naive, INTERNAL);

        out.append("\n== 3. A blocklist of names is easy to walk around ==\n");
        Importer blocklist = new BlocklistImporter(dns, net);
        line(out, "metadata address", blocklist, METADATA);
        line(out, "decimal spelling", blocklist, DECIMAL);
        line(out, "short loopback", blocklist, SHORT_LOOPBACK);
        line(out, "mapped IPv6 spelling", blocklist, MAPPED);
        line(out, "name that points inside", blocklist, DNS_NAME);
        line(out, "redirect from outside", blocklist, REDIRECT);

        out.append("\n== 4. Check the address the connection really uses ==\n");
        Importer safe = new SafeImporter(dns, net);
        line(out, "honest address", safe, HONEST);
        for (String[] attack : new String[][] {
                {"metadata address", METADATA}, {"localhost", LOCALHOST}, {"decimal spelling", DECIMAL},
                {"short loopback", SHORT_LOOPBACK}, {"mapped IPv6 spelling", MAPPED}, {"name that points inside", DNS_NAME},
                {"redirect from outside", REDIRECT}, {"user info trick", USER_INFO}, {"DNS rebinding (own image only)", REBINDING}}) {
            line(out, attack[0], safe, attack[1]);
        }
        line(out, "two answers, one private", safe, "http://dual.attacker.example/admin");

        out.append("\n== 5. Why checking and then connecting is not enough ==\n");
        out.append(checkThenFetch(Network.dns(), net)).append('\n');
        return out.toString();
    }

    /** A "check, then connect" importer that resolves the name twice: the second answer is not the one it checked. */
    static String checkThenFetch(Dns dns, Network net) {
        String host = "rebind.attacker.example";
        boolean looksPublic = dns.resolve(host).stream().allMatch(AddressPolicy::isPublic);
        String fetched = net.get(dns.resolve(host).get(0), "/admin").body();
        return "check-then-connect: checked as public=" + looksPublic + ", then fetched -> " + fetched;
    }

    private static void line(StringBuilder out, String label, Importer importer, String address) {
        out.append(String.format("%-31s -> %s%n", label, attempt(importer, address)));
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
        return List.of(METADATA, LOCALHOST, INTERNAL, DECIMAL, SHORT_LOOPBACK, MAPPED, DNS_NAME, REDIRECT, REBINDING, USER_INFO);
    }
}
