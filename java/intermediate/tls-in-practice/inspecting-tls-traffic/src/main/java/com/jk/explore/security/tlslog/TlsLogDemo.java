package com.jk.explore.security.tlslog;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.List;
import java.util.Map;

/** Runs the capture-and-key-log story in five numbered acts and prints exact, repeatable results. Keys come from a labelled generator so the bytes are the same every run. */
public class TlsLogDemo {

    static final List<String> LOGIN = List.of("POST /login HTTP/1.1 | user=ana&password=hunter2");
    static final List<String> LOGIN_REPLY = List.of("HTTP/1.1 200 OK | session=7f3a");
    static final List<String> ORDERS = List.of("GET /orders HTTP/1.1");
    static final List<String> ORDERS_REPLY = List.of("HTTP/1.1 200 OK | 3 orders");

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    /** The first {@code chars} hex digits, then "...". */
    static String hex(byte[] b, int chars) {
        return Hex.of(b).substring(0, chars) + "...";
    }

    /** Bytes as hex pairs: "17 03 03 00 41". */
    static String pairs(byte[] b, int from, int to) {
        StringBuilder s = new StringBuilder();
        for (int i = from; i < to; i++) {
            s.append(i > from ? " " : "").append(String.format("%02x", b[i]));
        }
        return s.toString();
    }

    private static void records(StringBuilder out, List<String> opened) {
        records(out, opened, "  ");
    }

    private static void records(StringBuilder out, List<String> opened, String indent) {
        if (opened.isEmpty()) {
            line(out, indent + "[] (nothing opens)");
        }
        opened.forEach(r -> line(out, indent + r));
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        Connection.Established one = Connection.ecdhe("session-one", LOGIN, LOGIN_REPLY);
        Capture cap = one.capture();
        act1(out, cap);
        String log = KeyLog.lines(cap, one.secrets());
        act2(out, cap, log);
        act3(out, cap, log);
        act4(out, cap, log);
        act5(out, cap);
        return out.toString();
    }

    static void act1(StringBuilder out, Capture cap) {
        line(out, "ONE. What the wire shows.");
        line(out, "THE CONNECTION (an online store's login, TLS 1.3 style):");
        line(out, "  client sends  " + LOGIN.get(0));
        line(out, "  server sends  " + LOGIN_REPLY.get(0));
        line(out, "THE CAPTURE (what anyone on the network records):");
        line(out, "  client random     " + hex(cap.clientRandom(), 16) + " (in the clear)");
        line(out, "  server random     " + hex(cap.serverRandom(), 16) + " (in the clear)");
        for (Packet p : cap.packets()) {
            line(out, "  " + (p.fromClient() ? "client -> server" : "server -> client") + "  " + p.bytes().length + " bytes, " + pairs(p.bytes(), 0, 5) + " ...");
        }
        byte[] r = cap.packets().get(0).bytes();
        int length = (r[3] & 0xff) << 8 | (r[4] & 0xff);
        int text = LOGIN.get(0).getBytes(StandardCharsets.UTF_8).length;
        line(out, "THE RECORD HEADER, WORKED OUT (client -> server):");
        line(out, "  " + pairs(r, 0, 1) + "       type " + (r[0] & 0xff) + ": application data");
        line(out, "  " + pairs(r, 1, 3) + "    version 3.3, the same in every TLS 1.3 record");
        line(out, "  " + pairs(r, 3, 5) + "    length " + length + " = " + text + " text + 1 inner type + " + (length - text - 1) + " tag");
        line(out, "  total    5 header + " + length + " = " + r.length + " bytes");
        line(out, "THE RESULT (with no key):");
        line(out, "  contains \"password\"?  " + contains(cap, "password"));
        line(out, "  decrypted             " + Decryptor.withoutKeys(cap));
    }

    static void act2(StringBuilder out, Capture cap, String log) {
        line(out, "TWO. Writing a key log.");
        line(out, "THE SETTINGS (the client's environment):");
        line(out, "  SSLKEYLOGFILE not set           " + KeyLog.pathFor(Map.of()).orElse("no key log"));
        line(out, "  SSLKEYLOGFILE, APP_ENV=dev      " + KeyLog.pathFor(Map.of("SSLKEYLOGFILE", "/tmp/keys.log", "APP_ENV", "dev")).orElse("no key log"));
        line(out, "  SSLKEYLOGFILE, APP_ENV=production");
        line(out, "    " + refusal(Map.of("SSLKEYLOGFILE", "/tmp/keys.log", "APP_ENV", "production")));
        line(out, "THE KEY LOG (two lines for this connection):");
        for (String l : log.split("\n")) {
            String[] p = l.split(" ");
            line(out, "  " + p[0] + " " + p[1].substring(0, 12) + "... " + p[2].substring(0, 12) + "...");
        }
        String[] first = log.split("\n")[0].split(" ");
        line(out, "THE LINE, PART BY PART:");
        line(out, "  label          " + first[0]);
        line(out, "  client random  " + first[1].substring(0, 12) + "... (" + first[1].length() / 2 + " bytes)");
        line(out, "                 the capture's: " + hex(cap.clientRandom(), 12) + " " + (first[1].equals(Hex.of(cap.clientRandom())) ? "same" : "different"));
        line(out, "  secret         " + first[2].substring(0, 12) + "... (" + first[2].length() / 2 + " bytes, one direction)");
    }

    static void act3(StringBuilder out, Capture cap, String log) {
        byte[] secret = KeyLog.find(log, "CLIENT_TRAFFIC_SECRET_0", cap.clientRandom()).orElseThrow();
        line(out, "THREE. Reading the capture with the key log.");
        line(out, "THE LOOKUP (the capture's client random in the log):");
        line(out, "  client random            " + hex(cap.clientRandom(), 12));
        line(out, "  CLIENT_TRAFFIC_SECRET_0  " + hex(secret, 12) + " found");
        line(out, "  SERVER_TRAFFIC_SECRET_0  " + KeyLog.find(log, "SERVER_TRAFFIC_SECRET_0", cap.clientRandom()).map(s -> hex(s, 12)).orElse("none") + " found");
        line(out, "FROM SECRET TO KEY, WORKED OUT (client direction):");
        line(out, "  key    Expand-Label(secret, \"key\", 16)  " + hex(Records.keyBytes(secret), 16));
        line(out, "  iv     Expand-Label(secret, \"iv\", 12)   " + hex(Records.iv(secret), 16));
        line(out, "  nonce  iv XOR record number: record 0 ends " + pairs(Records.nonce(secret, 0), 10, 12));
        line(out, "                               record 1 ends " + pairs(Records.nonce(secret, 1), 10, 12));
        line(out, "  AES-128-GCM opens the record, the header is checked too");
        line(out, "THE RESULT (the records, opened):");
        records(out, Decryptor.withKeyLog(cap, log));
    }

    static void act4(StringBuilder out, Capture cap, String log) {
        Connection.Established two = Connection.ecdhe("session-two", ORDERS, ORDERS_REPLY);
        line(out, "FOUR. A key log opens its own connections and no others.");
        line(out, "THE SECOND CONNECTION (another customer):");
        line(out, "  client random  " + hex(two.capture().clientRandom(), 12) + " (session one: " + hex(cap.clientRandom(), 12) + ")");
        line(out, "TRY 1: session two, with session one's key log");
        records(out, Decryptor.withKeyLog(two.capture(), log));
        line(out, "TRY 2: session two, with its own two lines added");
        records(out, Decryptor.withKeyLog(two.capture(), log + KeyLog.lines(two.capture(), two.secrets())));
        line(out, "TRY 3: session one, with only its CLIENT line");
        records(out, Decryptor.withKeyLog(cap, log.lines().filter(l -> l.startsWith("CLIENT")).findFirst().orElseThrow()));
        line(out, "THE RESULT:");
        line(out, "  a line opens one direction of one connection, no more");
    }

    static void act5(StringBuilder out, Capture cap) throws Exception {
        KeyPairGenerator rsa = KeyPairGenerator.getInstance("RSA");
        rsa.initialize(2048);
        KeyPair serverLongTerm = rsa.generateKeyPair();
        Capture old = Connection.staticRsa("recorded-last-year", serverLongTerm.getPublic(), LOGIN, LOGIN_REPLY).capture();
        line(out, "FIVE. Why the server's own key is a different matter.");
        line(out, "THE RECORDINGS (made last year):");
        line(out, "  static RSA  the client's secret, encrypted to the server key:");
        line(out, "              " + old.clientKeyShare().length + " bytes on the wire");
        line(out, "  ECDHE       two public values of " + cap.clientKeyShare().length + " and " + cap.serverKeyShare().length + " bytes:");
        line(out, "              " + hex(cap.clientKeyShare(), 12) + " and " + hex(cap.serverKeyShare(), 12));
        line(out, "              the shared secret is never sent");
        line(out, "THE LEAK (today): the server's long-term RSA private key");
        line(out, "THE RESULT:");
        line(out, "  static RSA, opened with the leaked key:");
        records(out, Decryptor.withServerLongTermKey(old, serverLongTerm.getPrivate()), "    ");
        line(out, "  ECDHE, opened with the same key:");
        records(out, Decryptor.withServerLongTermKey(cap, serverLongTerm.getPrivate()), "    ");
        line(out, "WHY, WORKED OUT:");
        line(out, "  static RSA  the key decrypts the " + old.clientKeyShare().length + " bytes: the secret");
        line(out, "  ECDHE       the secret needs a private value of the");
        line(out, "              handshake, thrown away when it ended");
    }

    private static boolean contains(Capture c, String word) {
        return c.packets().stream().anyMatch(p -> new String(p.bytes(), StandardCharsets.ISO_8859_1).contains(word));
    }

    private static String refusal(Map<String, String> env) {
        try {
            return String.valueOf(KeyLog.pathFor(env));
        } catch (IllegalStateException e) {
            return "refused: " + e.getMessage();
        }
    }
}
