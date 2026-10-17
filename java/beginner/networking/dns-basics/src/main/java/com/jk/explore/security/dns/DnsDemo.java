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
        line(out, "  the question for " + SHOP + " is " + query.length + " bytes: 12 of header, then the name as labels, then type and class");
        line(out, "  its first 12 bytes: " + HexFormat.of().formatHex(query, 0, 12));
        DnsMessage response = DnsMessage.query(0xabcd, SHOP).answeredWith(300, REAL);
        byte[] reply = response.encode();
        line(out, "  the answer repeats the name as a two byte pointer, so it is " + reply.length + " bytes and not " + (query.length + (SHOP.length() + 2) + 14));
        DnsMessage again = DnsMessage.decode(reply);
        line(out, "  decoded again: " + again.questions().get(0).name() + " is " + Address.text(again.answers().get(0).address())
                + " for " + again.answers().get(0).ttl() + " seconds");
        line(out, "  nothing in the message says who made the answer: only the id and the question have to match");
    }

    private static void act2Cache(StringBuilder out) {
        line(out, "TWO. The cache and the time to live.");
        Resolver resolver = new Resolver(TransactionIds.keyed(SECRET), 3600);
        Resolver.Pending pending = resolver.ask(SHOP, 0).orElseThrow();
        resolver.accept(pending, DnsMessage.query(pending.id(), SHOP).answeredWith(300, REAL), pending.port(), 0);
        line(out, "  at second 0 the answer " + Address.text(REAL) + " arrives with a time to live of 300 seconds");
        line(out, "  at second 100 the resolver answers from its cache: " + resolver.cached(SHOP, 100).isPresent() + ", questions sent upstream so far: " + resolver.upstreamQueries());
        line(out, "  at second 300 the entry has expired and the resolver asks again: " + resolver.ask(SHOP, 300).isPresent());
        line(out, "  caching makes DNS fast, and it also means one false answer is repeated to every customer until it expires");
    }

    private static void act3Exchange(StringBuilder out) throws IOException, InterruptedException, BadMessageException {
        line(out, "THREE. A real exchange over UDP on this machine.");
        LoopbackExchange.Result result = LoopbackExchange.ask(SHOP, 0x1234, 300, REAL);
        line(out, "  the client sent one datagram of " + result.queryBytes() + " bytes and got one of " + result.responseBytes() + " bytes back");
        line(out, "  the answer arrived: " + result.answered() + ", repeats the id: " + result.idRepeated() + ", says " + Address.text(result.address()));
        line(out, "  the name can be read in the query as plain text: " + result.nameReadableOnTheWire() + "  <- ANYONE ON THE PATH SEES WHICH SITES YOU ASK FOR");
        line(out, "  there is no connection and no signature: a datagram is believed if it looks right");
    }

    private static void act4Poisoning(StringBuilder out) {
        line(out, "FOUR. Cache poisoning by guessing.");
        out.append(poison("ids that count up, all queries from port 53", TransactionIds.sequential(4000, 53), 53, 4001));
        out.append(poison("ids and ports from a keyed hash", TransactionIds.keyed(SECRET), 33333, 0));
        long space = (long) 65536 * TransactionIds.PORTS;
        line(out, "  with a keyed id and port the attacker must match one of " + space + " pairs, and " + GUESSES + " guesses are about one in "
                + (space / GUESSES));
    }

    private static String poison(String how, TransactionIds ids, int guessedPort, int guessedFirstId) {
        Resolver resolver = new Resolver(ids, 86400);
        resolver.ask(WWW, 0).orElseThrow();
        Resolver.Pending pending = resolver.ask(SHOP, 0).orElseThrow();
        boolean hit = false;
        for (int i = 0; i < GUESSES && !hit; i++) {
            DnsMessage forged = DnsMessage.query((guessedFirstId + i) & 0xffff, SHOP).answeredWith(86400, ATTACKER);
            hit = resolver.accept(pending, forged, guessedPort, 0);
        }
        Optional<Integer> now = resolver.cached(SHOP, 1);
        StringBuilder out = new StringBuilder();
        line(out, "  the resolver uses " + how + ": after " + GUESSES + " forged answers the cache says "
                + now.map(Address::text).orElse("nothing") + (hit ? "  <- THE ATTACKER'S ADDRESS" : ""));
        return out.toString();
    }

    private static void act5Lifetime(StringBuilder out) {
        line(out, "FIVE. How long a false answer lives, and what stops it.");
        for (long cap : new long[] {86400, 3600}) {
            Resolver resolver = new Resolver(TransactionIds.sequential(1, 53), cap);
            Resolver.Pending pending = resolver.ask(SHOP, 0).orElseThrow();
            resolver.accept(pending, DnsMessage.query(pending.id(), SHOP).answeredWith(86400, ATTACKER), 53, 0);
            line(out, "  a false answer with a time to live of 86400 seconds, and the resolver caps lifetimes at " + cap
                    + ": still believed after 2 hours " + resolver.cached(SHOP, 7200).isPresent());
        }
        line(out, "  DNSSEC signs the answers, so a forged one fails a check that needs no trust in the path (described here, not run)");
        line(out, "  DNS over TLS or HTTPS hides the questions and protects the path to the resolver, but not the data upstream");
        line(out, "  the browser's check that the site's certificate names shop.example.com is the last line of defence, which is the transport-security module");
    }
}
