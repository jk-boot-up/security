package com.jk.explore.security.dns;

import java.io.IOException;
import java.util.HexFormat;
import java.util.Optional;

/** Runs the online store story in five numbered acts and prints exact, repeatable results. */
public class DnsDemo {

    static final String SHOP = "shop.example.com";
    static final String WWW = "www.shop.example.com";
    static final int REAL = Address.parse("203.0.113.10");
    static final int ATTACKER = Address.parse("192.0.2.66");
    static final String SECRET = "demo-secret-for-resolver-ids";
    static final int GUESSES = 1000;

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() throws IOException, InterruptedException, BadMessageException {
        StringBuilder out = new StringBuilder();
        act1Message(out);
        act2Cache(out);
        act3Exchange(out);
        act4Poisoning(out);
        act5Lifetime(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void act1Message(StringBuilder out) throws BadMessageException {
        line(out, "ONE. A DNS message, byte by byte.");
        byte[] query = DnsMessage.query(0xabcd, SHOP).encode();
        int nameEnd = 12 + SHOP.length() + 2;                                  // labels + the closing 00
        line(out, "  THE QUESTION (" + query.length + " bytes):");
        line(out, "    header   " + words(query, 0, 12));
        line(out, "    name     " + labels(query, 12));
        line(out, "    type     " + hex(query, nameEnd, 2) + " = A, an IPv4 address");
        line(out, "    class    " + hex(query, nameEnd + 2, 2) + " = IN, the internet");
        byte[] reply = DnsMessage.query(0xabcd, SHOP).answeredWith(300, REAL).encode();
        int at = query.length;                                                 // the answer record starts here
        line(out, "  THE ANSWER (" + reply.length + " bytes):");
        line(out, "    header   " + words(reply, 0, 12));
        line(out, "    question the same " + (query.length - 12) + " bytes as above");
        line(out, "    name     " + hex(reply, at, 2) + " = a pointer to byte " + (((reply[at] & 0x3f) << 8) | (reply[at + 1] & 0xff)));
        line(out, "    type     " + hex(reply, at + 2, 2) + "  class " + hex(reply, at + 4, 2));
        line(out, "    ttl      " + hex(reply, at + 6, 4) + " = " + DnsMessage.decode(reply).answers().get(0).ttl() + " seconds");
        line(out, "    address  " + hex(reply, at + 10, 2) + " " + hex(reply, at + 12, 4) + " = " + Address.text(REAL));
        line(out, "  without the pointer the answer would be " + (reply.length - 2 + SHOP.length() + 2) + " bytes, not " + reply.length);
        line(out, "  no sender in it: only the id and question must match");
    }

    static String hex(byte[] bytes, int from, int length) {
        return HexFormat.of().formatHex(bytes, from, from + length);
    }

    /** "abcd 0100 0001 ...": the header as its six 2-byte fields. */
    static String words(byte[] bytes, int from, int to) {
        StringBuilder text = new StringBuilder();
        for (int i = from; i < to; i += 2) {
            text.append(i > from ? " " : "").append(hex(bytes, i, 2));
        }
        return text.toString();
    }

    /** "04 73686f70 07 ...": each label as its length byte, then its letters, up to the closing 00. */
    static String labels(byte[] bytes, int from) {
        StringBuilder text = new StringBuilder();
        int at = from;
        while (bytes[at] != 0) {
            text.append(hex(bytes, at, 1)).append(' ').append(hex(bytes, at + 1, bytes[at])).append(' ');
            at += 1 + bytes[at];
        }
        return text.append("00").toString();
    }

    private static void act2Cache(StringBuilder out) {
        line(out, "TWO. The cache and the time to live.");
        long cap = 3600;
        Resolver resolver = new Resolver(TransactionIds.keyed(SECRET), cap);
        Resolver.Pending pending = resolver.ask(SHOP, 0).orElseThrow();
        resolver.accept(pending, DnsMessage.query(pending.id(), SHOP).answeredWith(300, REAL), pending.port(), 0);
        line(out, "  THE ANSWER (second 0):");
        line(out, "    " + SHOP + "  " + Address.text(REAL) + "  ttl 300");
        line(out, "    kept until 0 + min(300, cap " + cap + ") = second " + resolver.expiresAt(SHOP).orElseThrow());
        line(out, "  LATER QUESTIONS:");
        for (long second : new long[] {100, 299, 300}) {
            Optional<Integer> hit = resolver.cached(SHOP, second);
            if (hit.isPresent()) {
                line(out, "    second " + second + "  from the cache  " + Address.text(hit.get()) + "  upstream " + resolver.upstreamQueries());
            } else {
                resolver.ask(SHOP, second);
                line(out, "    second " + second + "  expired, asked again          upstream " + resolver.upstreamQueries());
            }
        }
        line(out, "  a false answer goes to every customer until it expires");
    }

    private static void act3Exchange(StringBuilder out) throws IOException, InterruptedException, BadMessageException {
        line(out, "THREE. A real exchange over UDP on this machine.");
        int id = 0x1234;
        LoopbackExchange.Result result = LoopbackExchange.ask(SHOP, id, 300, REAL);
        line(out, "  THE QUERY (one datagram, " + result.queryBytes() + " bytes):");
        line(out, "    id " + Integer.toHexString(id) + ", asks for " + SHOP);
        line(out, "    seen on the path: " + result.seenOnThePath());
        line(out, "    name readable: " + result.nameReadableOnTheWire() + "  <- THE PATH SEES IT");
        line(out, "  THE RESPONSE (one datagram, " + result.responseBytes() + " bytes):");
        line(out, "    arrived: " + result.answered() + ", repeats id " + Integer.toHexString(id) + ": " + result.idRepeated());
        line(out, "    says " + Address.text(result.address()));
        line(out, "  no connection and no signature on the reply");
    }

    private static void act4Poisoning(StringBuilder out) {
        line(out, "FOUR. Cache poisoning by guessing.");
        line(out, "  THE ATTACK: up to " + GUESSES + " forged answers, each says " + Address.text(ATTACKER));
        line(out, "  BEFORE THE FIX: ids count up, every query from port 53");
        line(out, "    last id seen 4000, so the guess is id 4001, port 53");
        out.append(poison(TransactionIds.sequential(4000, 53), 53, 4001));
        line(out, "  AFTER THE FIX: id and port from a keyed hash of a counter");
        TransactionIds.Keyed keyed = TransactionIds.derive(SECRET, 2);
        byte[] h = keyed.hash();
        line(out, "    HMAC-SHA-256(secret, 2) = " + hex(h, 0, 2) + " " + hex(h, 2, 4) + " " + hex(h, 6, 4) + " ...");
        line(out, "    id    first 2 bytes " + hex(h, 0, 2) + " = " + keyed.id());
        line(out, "    port  1024 + (" + hex(h, 2, 4) + " mod " + TransactionIds.PORTS + ") = " + keyed.port());
        out.append(poison(TransactionIds.keyed(SECRET), 53, 4001));
        long space = (long) 65536 * TransactionIds.PORTS;
        line(out, "  1 of 65536 ids x " + TransactionIds.PORTS + " ports = " + space + " pairs");
        line(out, "  " + GUESSES + " guesses hit about 1 time in " + (space / GUESSES));
    }

    /** The resolver asks for www first (query 1), then for the store (query 2), which the attacker races. */
    private static String poison(TransactionIds ids, int guessedPort, int guessedFirstId) {
        Resolver resolver = new Resolver(ids, 86400);
        resolver.ask(WWW, 0).orElseThrow();
        Resolver.Pending pending = resolver.ask(SHOP, 0).orElseThrow();
        int sent = 0;
        boolean hit = false;
        while (sent < GUESSES && !hit) {
            DnsMessage forged = DnsMessage.query((guessedFirstId + sent++) & 0xffff, SHOP).answeredWith(86400, ATTACKER);
            hit = resolver.accept(pending, forged, guessedPort, 0);
        }
        Optional<Integer> now = resolver.cached(SHOP, 1);
        StringBuilder out = new StringBuilder();
        line(out, "    after " + sent + " forged answer" + (sent == 1 ? "" : "s") + " the cache says "
                + now.map(Address::text).orElse("nothing") + (hit ? "  <- POISONED" : ""));
        return out.toString();
    }

    private static void act5Lifetime(StringBuilder out) {
        line(out, "FIVE. How long a false answer lives, and what stops it.");
        line(out, "  THE FALSE ANSWER: " + Address.text(ATTACKER) + ", keep it 86400 seconds (a day)");
        String[] labels = {"BEFORE THE FIX: cap 86400 (a day)", "AFTER THE FIX: cap 3600 (an hour)"};
        long[] caps = {86400, 3600};
        for (int i = 0; i < caps.length; i++) {
            Resolver resolver = new Resolver(TransactionIds.sequential(1, 53), caps[i]);
            Resolver.Pending pending = resolver.ask(SHOP, 0).orElseThrow();
            resolver.accept(pending, DnsMessage.query(pending.id(), SHOP).answeredWith(86400, ATTACKER), 53, 0);
            line(out, "  " + labels[i]);
            line(out, "    kept until min(86400, " + caps[i] + ") = second " + resolver.expiresAt(SHOP).orElseThrow());
            line(out, "    still believed after 2 hours (second 7200): " + resolver.cached(SHOP, 7200).isPresent());
        }
        line(out, "  WHAT ELSE STOPS IT (described here, not run):");
        line(out, "    DNSSEC: a forged answer fails the signature check");
        line(out, "    DNS over TLS or HTTPS: hides the questions on the path");
        line(out, "    TLS: the certificate must name " + SHOP);
    }
}
