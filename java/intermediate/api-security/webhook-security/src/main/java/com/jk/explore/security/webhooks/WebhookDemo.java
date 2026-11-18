package com.jk.explore.security.webhooks;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Runs the online store story in six numbered acts and prints exact, repeatable results: each message and its response in separate labelled
 * sections, and the signature worked out step by step. The clock is simulated, so every time and signature is the same on every run; ports are
 * never printed.
 */
public class WebhookDemo {

    static final byte[] SECRET = "whsec-demo-shop-2026".getBytes(StandardCharsets.UTF_8);
    static final byte[] NEW_SECRET = "whsec-demo-shop-2027".getBytes(StandardCharsets.UTF_8);
    static final byte[] OTHER_SECRET = "whsec-not-the-provider".getBytes(StandardCharsets.UTF_8);
    static final long START = 1_800_000_000L;

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() throws IOException, InterruptedException {
        StringBuilder out = new StringBuilder();
        act1Trusting(out);
        act2Signing(out);
        act3Checking(out);
        act4Replays(out);
        act5Idempotency(out);
        act6TheBill(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    static Orders orders() {
        return new Orders().add("A-1001", 4200).add("A-1002", 1999);
    }

    static Reply post(URI endpoint, byte[] body, String signature) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(endpoint).POST(HttpRequest.BodyPublishers.ofByteArray(body));
        if (signature != null) {
            request.header(Signatures.HEADER, signature);
        }
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
            return new Reply(response.statusCode(), response.body());
        }
    }

    /** A JSON body on two lines, split before the order field, so it fits a slide; the bytes themselves are unchanged. */
    private static void body(StringBuilder out, String indent, byte[] raw) {
        String text = new String(raw, StandardCharsets.UTF_8);
        int split = text.indexOf("\"order\"");
        line(out, indent + text.substring(0, split).stripTrailing());
        line(out, indent + text.substring(split));
    }

    /** The v1 part of a signature header. */
    private static String v1(String header) {
        return header.substring(header.indexOf("v1=") + 3);
    }

    private static String bytes(byte[] value, int count) {
        return HexFormat.ofDelimiter(" ").formatHex(Arrays.copyOf(value, count)) + " ...";
    }

    private static void act1Trusting(StringBuilder out) throws IOException, InterruptedException {
        line(out, "ONE. An endpoint that trusts whatever is posted to it.");
        SimClock clock = new SimClock(START);
        Provider provider = new Provider(clock, SECRET);
        Orders orders = orders();
        try (WebhookServer server = new WebhookServer(new Receiver(Receiver.Policy.trusting(), clock, orders))) {
            byte[] forged = new Event("evt_forged1", Event.PAID, "A-1001", 4200).toJson();
            line(out, "  THE REQUEST (posted by anyone, with no signature):");
            line(out, "    POST /webhooks/payments");
            body(out, "    ", forged);
            line(out, "  THE RESPONSE:");
            line(out, "    " + post(server.endpoint(), forged, null));
            line(out, "  THE PROVIDER'S OWN RECORDS FOR A-1001:");
            line(out, "    " + provider.status("A-1001") + "  <- MARKED PAID, NEVER PAID");
            line(out, "  the address is public: a message to it proves nothing");
        }
    }

    private static void act2Signing(StringBuilder out) {
        line(out, "TWO. How the provider signs a message.");
        Event event = new Event("evt_1001paid", Event.PAID, "A-1001", 4200);
        byte[] raw = event.toJson();
        line(out, "  THE INPUTS:");
        line(out, "    secret  " + new String(SECRET, StandardCharsets.UTF_8) + "  (" + SECRET.length + " bytes, shared)");
        line(out, "    time t  " + START + "  (seconds since 1 January 1970)");
        line(out, "    raw body (" + raw.length + " bytes):");
        body(out, "      ", raw);
        line(out, "  STEP 1, WHAT IS SIGNED: t, a dot, the raw body");
        body(out, "    ", Signatures.signedContent(START, raw));
        HmacSteps.Steps steps = HmacSteps.of(SECRET, Signatures.signedContent(START, raw));
        line(out, "  STEP 2, THE KEY PADDED WITH ZEROS TO 64 BYTES, THEN MIXED:");
        line(out, "    padded key   " + bytes(steps.paddedKey(), 8));
        line(out, "    XOR 36 each  " + bytes(steps.innerKey(), 8) + "  inner key");
        line(out, "    XOR 5c each  " + bytes(steps.outerKey(), 8) + "  outer key");
        String inner = HexFormat.of().formatHex(steps.inner());
        String mac = HexFormat.of().formatHex(steps.mac());
        line(out, "  STEP 3, TWO SHA-256 HASHES:");
        line(out, "    inner = SHA-256(inner key + what is signed)");
        line(out, "          = " + inner.substring(0, 32));
        line(out, "            " + inner.substring(32));
        line(out, "    v1    = SHA-256(outer key + inner)");
        line(out, "          = " + mac.substring(0, 32));
        line(out, "            " + mac.substring(32));
        String header = Signatures.header(SECRET, START, raw);
        line(out, "  THE HEADER SENT WITH THE BODY (" + Signatures.HEADER + "):");
        line(out, "    " + header.substring(0, header.indexOf(',') + 1));
        line(out, "    v1=" + v1(header).substring(0, 32));
        line(out, "       " + v1(header).substring(32));
        line(out, "  the same as the JDK's Mac HmacSHA256: " + mac.equals(v1(header)));
    }

    private static void check(StringBuilder out, Receiver receiver, String what, byte[] body, String header) {
        line(out, "    " + what);
        if (header != null) {
            line(out, "      v1 sent " + v1(header).substring(0, 16) + "...  computed "
                    + v1(Signatures.header(SECRET, START, body)).substring(0, 16) + "...");
        }
        line(out, "      " + receiver.receive(body, header));
    }

    private static void act3Checking(StringBuilder out) throws IOException, InterruptedException {
        line(out, "THREE. The store checks the signature.");
        SimClock clock = new SimClock(START);
        Provider provider = new Provider(clock, SECRET);
        Receiver receiver = new Receiver(Receiver.Policy.signedOnly(), clock, orders(), SECRET);
        Provider.Delivery real = provider.send(new Event("evt_1001paid", Event.PAID, "A-1001", 4200));
        byte[] changed = new String(real.body(), StandardCharsets.UTF_8).replace("A-1001", "A-1002").getBytes(StandardCharsets.UTF_8);
        byte[] forged = new Event("evt_forged2", Event.PAID, "A-1002", 1999).toJson();
        line(out, "  THE STORE'S STEPS:");
        line(out, "    read t and v1 from the header");
        line(out, "    compute the HMAC of t, a dot and the raw body received");
        line(out, "    compare it with v1 in constant time (MessageDigest.isEqual)");
        line(out, "  THE MESSAGES AND RESPONSES:");
        check(out, receiver, "1 the real event, as signed", real.body(), real.signature());
        check(out, receiver, "2 the body changed to A-1002, the same header", changed, real.signature());
        check(out, receiver, "3 a forged event with no header", forged, null);
        check(out, receiver, "4 a forged event signed with another secret", forged, Signatures.header(OTHER_SECRET, clock.now(), forged));
        Receiver overHttp = new Receiver(Receiver.Policy.signedOnly(), clock, orders(), SECRET);
        try (WebhookServer server = new WebhookServer(overHttp)) {
            line(out, "  THE SAME OVER THE LOCAL HTTP SERVER (POST only, 4096 bytes):");
            line(out, "    1 real " + post(server.endpoint(), real.body(), real.signature()).status()
                    + ", 2 changed " + post(server.endpoint(), changed, real.signature()).status()
                    + ", 3 no header " + post(server.endpoint(), forged, null).status());
        }
    }

    private static void act4Replays(StringBuilder out) {
        line(out, "FOUR. A real message sent again.");
        SimClock clock = new SimClock(START);
        Provider provider = new Provider(clock, SECRET);
        Orders orders = orders();
        Receiver signedOnly = new Receiver(Receiver.Policy.signedOnly(), clock, orders, SECRET);
        Provider.Delivery paid = provider.send(new Event("evt_1001paid", Event.PAID, "A-1001", 4200));
        signedOnly.receive(paid.body(), paid.signature());
        Provider.Delivery refund = provider.send(new Event("evt_1001refund", Event.REFUNDED, "A-1001", 4200));
        line(out, "  THE MESSAGE (a real refund, " + refund.signature().substring(0, refund.signature().indexOf(',')) + "):");
        body(out, "    ", refund.body());
        line(out, "  WITH SIGNATURES ONLY:");
        line(out, "    the real refund, at t + 0 seconds:");
        line(out, "      " + signedOnly.receive(refund.body(), refund.signature()));
        clock.advance(3600);
        line(out, "    a copy, sent again at t + 3600 seconds:");
        line(out, "      " + signedOnly.receive(refund.body(), refund.signature()));
        line(out, "    refunds paid: " + orders.refunds("A-1001") + ", " + orders.refundedCents("A-1001")
                + " cents for a 4200-cent order  <- REPLAYED");

        clock = new SimClock(START);
        provider = new Provider(clock, SECRET);
        orders = orders();
        Receiver complete = new Receiver(Receiver.Policy.complete(), clock, orders, SECRET);
        paid = provider.send(new Event("evt_1001paid", Event.PAID, "A-1001", 4200));
        complete.receive(paid.body(), paid.signature());
        refund = provider.send(new Event("evt_1001refund", Event.REFUNDED, "A-1001", 4200));
        line(out, "  WITH A 300-SECOND WINDOW AND A RECORD OF EVENT IDS:");
        line(out, "    the real refund, at t + 0 seconds:");
        line(out, "      " + complete.receive(refund.body(), refund.signature()));
        clock.advance(30);
        line(out, "    the copy at t + 30 (age 30 <= 300, id already seen):");
        line(out, "      " + complete.receive(refund.body(), refund.signature()));
        clock.advance(3600);
        long age = clock.now() - START;
        line(out, "    the copy at t + " + age + " (age " + age + " > 300):");
        line(out, "      " + complete.receive(refund.body(), refund.signature()));
        String newTime = refund.signature().replaceFirst("t=[0-9]+", "t=" + clock.now());
        line(out, "    the copy with its t edited to " + clock.now() + " (age 0):");
        line(out, "      " + complete.receive(refund.body(), newTime));
        line(out, "    refunds paid: " + orders.refunds("A-1001") + ", " + orders.refundedCents("A-1001") + " cents");
        line(out, "  the time is signed too, so it cannot be changed");
    }

    private static void act5Idempotency(StringBuilder out) {
        line(out, "FIVE. The same event twice, out of order, and a new secret.");
        SimClock clock = new SimClock(START);
        Provider provider = new Provider(clock, SECRET);
        Orders orders = orders();
        Receiver receiver = new Receiver(Receiver.Policy.complete(), clock, orders, SECRET);
        Provider.Delivery paid1001 = provider.send(new Event("evt_1001paid", Event.PAID, "A-1001", 4200));
        Provider.Delivery refund1002 = provider.send(new Event("evt_1002refund", Event.REFUNDED, "A-1002", 1999));
        Provider.Delivery paid1002 = provider.send(new Event("evt_1002paid", Event.PAID, "A-1002", 1999));
        Provider.Delivery[] arrivals = {paid1001, paid1001, refund1002, paid1002, refund1002};
        String[] notes = {"", "  (a retry)", "  (before its payment)", "", "  (a retry)"};
        line(out, "  THE EVENTS, IN THE ORDER THEY ARRIVE:");
        for (int i = 0; i < arrivals.length; i++) {
            Event e = Event.parse(arrivals[i].body()).orElseThrow();
            line(out, String.format("    %d %-15s%s %s%s", i + 1, e.id(), e.order(), e.type(), notes[i]));
        }
        line(out, "  THE RESPONSES (event, the order's state before, reply):");
        for (int i = 0; i < arrivals.length; i++) {
            String order = Event.parse(arrivals[i].body()).orElseThrow().order();
            String before = orders.state(order).name().toLowerCase(Locale.ROOT);
            line(out, String.format("    %d %-5s%s", i + 1, before, receiver.receive(arrivals[i].body(), arrivals[i].signature())));
        }
        line(out, "  the orders changed " + orders.changes() + " times; A-1001 is "
                + orders.state("A-1001").name().toLowerCase(Locale.ROOT) + ", A-1002 is "
                + orders.state("A-1002").name().toLowerCase(Locale.ROOT));

        SimClock rotation = new SimClock(START);
        Provider shop = new Provider(rotation, SECRET);
        Receiver store = new Receiver(Receiver.Policy.complete(), rotation, orders(), SECRET);
        Provider.Delivery signedBefore = shop.send(new Event("evt_old", Event.PAID, "A-1001", 4200));
        store.addSecret(NEW_SECRET);
        shop.rotateTo(NEW_SECRET);
        Provider.Delivery signedAfter = shop.send(new Event("evt_new", Event.PAID, "A-1002", 1999));
        String oldName = new String(SECRET, StandardCharsets.UTF_8);
        String newName = new String(NEW_SECRET, StandardCharsets.UTF_8);
        line(out, "  CHANGING THE SECRET, THE SECRETS THE STORE ACCEPTS:");
        line(out, "    before      " + oldName);
        line(out, "    changeover  " + oldName + ", " + newName);
        line(out, "    after       " + newName);
        line(out, "  THE MESSAGES AND RESPONSES:");
        line(out, "    changeover, signed with " + oldName + ":");
        line(out, "      " + store.receive(signedBefore.body(), signedBefore.signature()));
        line(out, "    changeover, signed with " + newName + ":");
        line(out, "      " + store.receive(signedAfter.body(), signedAfter.signature()));
        store.removeSecret(SECRET);
        Provider.Delivery late = new Provider(rotation, SECRET).send(new Event("evt_late", Event.REFUNDED, "A-1001", 4200));
        line(out, "    after, signed with " + oldName + ":");
        line(out, "      " + store.receive(late.body(), late.signature()));
    }

    private static void act6TheBill(StringBuilder out) {
        line(out, "SIX. The bill.");
        SimClock clock = new SimClock(START);
        Provider provider = new Provider(clock, SECRET);
        Receiver receiver = new Receiver(Receiver.Policy.complete(), clock, orders(), SECRET);
        Event event = new Event("evt_1001paid", Event.PAID, "A-1001", 4200);
        Provider.Delivery real = provider.send(event);
        byte[] pretty = event.toPrettyJson();
        line(out, "  THE RAW BODY, AS SIGNED (" + real.body().length + " bytes):");
        body(out, "    ", real.body());
        line(out, "  THE SAME EVENT, PARSED AND WRITTEN AGAIN (" + pretty.length + " bytes):");
        body(out, "    ", pretty);
        line(out, "  THE RESPONSES (the header's v1 is " + v1(real.signature()).substring(0, 16) + "...):");
        line(out, "    written again: computed v1 " + v1(Signatures.header(SECRET, START, pretty)).substring(0, 16) + "...");
        line(out, "      " + receiver.receive(pretty, real.signature()));
        line(out, "    raw body:      computed v1 " + v1(Signatures.header(SECRET, START, real.body())).substring(0, 16) + "...");
        line(out, "      " + receiver.receive(real.body(), real.signature()));
        line(out, "  THE PROVIDER'S API, ASKED BEFORE SHIPPING A-1001:");
        line(out, "    " + provider.status("A-1001") + ", which matches the event");
        line(out, "  keep the raw bytes, and check them before parsing");
        line(out, "  keep the secret outside the code; change it on a schedule");
        line(out, "  events come late, twice or out of order: handlers are idempotent");
    }
}
