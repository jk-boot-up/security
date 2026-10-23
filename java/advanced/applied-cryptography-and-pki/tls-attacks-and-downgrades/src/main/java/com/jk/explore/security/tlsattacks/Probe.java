package com.jk.explore.security.tlsattacks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** What one openssl s_client connection did. */
record Probe(boolean connected, String protocol, String cipher, String group, int dhBits, String alert, String raw) {

    private static final Pattern ALERT = Pattern.compile("alert ([a-z ]+?):ssl/record");
    private static final Pattern DH = Pattern.compile("Peer Temp Key: DH, (\\d+) bits");
    private static final Pattern GROUP = Pattern.compile("Negotiated TLS1.3 group: (\\S+)");

    /** Client flags that let a modern s_client talk to old servers: the security level is lowered to 0 for this one command. */
    static final String OLD_OK = "DEFAULT:NULL-SHA:@SECLEVEL=0";

    static Probe parse(String text) {
        boolean ok = text.contains("CONNECTION ESTABLISHED");
        String protocol = line(text, "Protocol version: ");
        String cipher = line(text, "Ciphersuite: ");
        Matcher g = GROUP.matcher(text);
        Matcher d = DH.matcher(text);
        Matcher a = ALERT.matcher(text);
        return new Probe(ok, protocol, cipher, g.find() ? g.group(1) : "", d.find() ? Integer.parseInt(d.group(1)) : 0,
                a.find() ? a.group(1) : "", text);
    }

    private static String line(String text, String prefix) {
        for (String l : text.split("\n")) {
            if (l.startsWith(prefix)) {
                return l.substring(prefix.length()).trim();
            }
        }
        return "";
    }

    /** Connects once to 127.0.0.1:port with openssl s_client and the extra flags. */
    static Probe run(Openssl openssl, int port, String input, String... flags) throws IOException, InterruptedException {
        List<String> args = new ArrayList<>(List.of("s_client", "-connect", "127.0.0.1:" + port, "-brief"));
        args.addAll(List.of(flags));
        return parse(openssl.run(input, args.toArray(String[]::new)).output());
    }

    /** Connects with a single protocol version and an optional cipher suite list (the weak settings are allowed on the client). */
    static Probe with(Openssl openssl, int port, String version, String cipher) throws IOException, InterruptedException {
        List<String> flags = new ArrayList<>();
        flags.add(version);
        flags.add("-cipher");
        flags.add(cipher + ":@SECLEVEL=0");
        return run(openssl, port, "", flags.toArray(String[]::new));
    }
}
