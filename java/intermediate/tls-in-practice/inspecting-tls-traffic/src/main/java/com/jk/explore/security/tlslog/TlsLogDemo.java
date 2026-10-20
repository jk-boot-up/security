package com.jk.explore.security.tlslog;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.List;
import java.util.Map;

/** Runs the capture-and-key-log story in five numbered acts and prints exact, repeatable results. Keys come from a labelled generator so the bytes are the same every run. */
public class TlsLogDemo {

    static final List<String> LOGIN = List.of("POST /login HTTP/1.1 | user=ana&password=hunter2");
    static final List<String> LOGIN_REPLY = List.of("HTTP/1.1 200 OK | session=7f3a");

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        Connection.Established one = Connection.ecdhe("session-one", LOGIN, LOGIN_REPLY);
        Capture cap = one.capture();

        out.append("== 1. What the wire shows ==\n");
        out.append("hello values in the clear -> client random ").append(Hex.of(cap.clientRandom()).substring(0, 16)).append("..., server random ")
                .append(Hex.of(cap.serverRandom()).substring(0, 16)).append("...\n");
        for (Packet p : cap.packets()) {
            out.append(p.fromClient() ? "client -> server" : "server -> client").append(": ").append(p.bytes().length).append(" bytes, starts ")
                    .append(Hex.of(p.bytes()).substring(0, 10)).append(" (application data)\n");
        }
        out.append("contains the word \"password\"? ").append(contains(cap, "password")).append('\n');
        out.append("decrypted without any key -> ").append(Decryptor.withoutKeys(cap)).append('\n');

        out.append("\n== 2. Writing a key log ==\n");
        out.append("SSLKEYLOGFILE not set -> ").append(KeyLog.pathFor(Map.of())).append('\n');
        out.append("SSLKEYLOGFILE=/tmp/keys.log in development -> ").append(KeyLog.pathFor(Map.of("SSLKEYLOGFILE", "/tmp/keys.log", "APP_ENV", "dev"))).append('\n');
        out.append("SSLKEYLOGFILE=/tmp/keys.log in production -> ").append(refusal(Map.of("SSLKEYLOGFILE", "/tmp/keys.log", "APP_ENV", "production"))).append('\n');
        String log = KeyLog.lines(cap, one.secrets());
        for (String line : log.split("\n")) {
            String[] p = line.split(" ");
            out.append("  ").append(p[0]).append(' ').append(p[1], 0, 12).append("... ").append(p[2], 0, 12).append("...\n");
        }

        out.append("\n== 3. Reading the capture with the key log ==\n");
        Decryptor.withKeyLog(cap, log).forEach(l -> out.append("  ").append(l).append('\n'));

        out.append("\n== 4. A key log opens its own connections and no others ==\n");
        Connection.Established two = Connection.ecdhe("session-two", List.of("GET /orders HTTP/1.1"), List.of("HTTP/1.1 200 OK | 3 orders"));
        out.append("session two with session one's key log -> ").append(Decryptor.withKeyLog(two.capture(), log)).append('\n');
        String both = log + KeyLog.lines(two.capture(), two.secrets());
        out.append("session two with its own lines -> ").append(Decryptor.withKeyLog(two.capture(), both)).append('\n');
        String clientOnly = log.lines().filter(l -> l.startsWith("CLIENT")).findFirst().orElseThrow();
        out.append("only the client line of session one -> ").append(Decryptor.withKeyLog(cap, clientOnly)).append('\n');

        out.append("\n== 5. Why the server's own key is a different matter ==\n");
        KeyPairGenerator rsa = KeyPairGenerator.getInstance("RSA");
        rsa.initialize(2048);
        KeyPair serverLongTerm = rsa.generateKeyPair();
        Connection.Established old = Connection.staticRsa("recorded-last-year", serverLongTerm.getPublic(), LOGIN, LOGIN_REPLY);
        out.append("recorded last year, static RSA key exchange; the server key leaks today -> ")
                .append(Decryptor.withServerLongTermKey(old.capture(), serverLongTerm.getPrivate())).append('\n');
        out.append("recorded last year, ephemeral key exchange; the same key leaks today -> ")
                .append(Decryptor.withServerLongTermKey(cap, serverLongTerm.getPrivate())).append('\n');
        return out.toString();
    }

    private static boolean contains(Capture c, String word) {
        return c.packets().stream().anyMatch(p -> new String(p.bytes(), java.nio.charset.StandardCharsets.ISO_8859_1).contains(word));
    }

    private static String refusal(Map<String, String> env) {
        try {
            return String.valueOf(KeyLog.pathFor(env));
        } catch (IllegalStateException e) {
            return "refused: " + e.getMessage();
        }
    }
}
