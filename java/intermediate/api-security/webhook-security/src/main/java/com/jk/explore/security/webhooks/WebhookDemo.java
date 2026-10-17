package com.jk.explore.security.webhooks;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Runs the online store story in five numbered acts and prints exact, repeatable results. Ports are never printed. */
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
        act2Signatures(out);
        act3Replays(out);
        act4Idempotency(out);
        act5TheBill(out);
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

    private static void act1Trusting(StringBuilder out) throws IOException, InterruptedException {
        line(out, "ONE. An endpoint that trusts whatever is posted to it.");
        SimClock clock = new SimClock(START);
        Provider provider = new Provider(clock, SECRET);
        Orders orders = orders();
        try (WebhookServer server = new WebhookServer(new Receiver(Receiver.Policy.trusting(), clock, orders))) {
            byte[] forged = new Event("evt_forged1", Event.PAID, "A-1001", 4200).toJson();
            line(out, "  anyone posts {\"type\":\"payment.succeeded\",\"order\":\"A-1001\"} to the store's webhook address, with no signature");
            line(out, "  the store answers: " + post(server.endpoint(), forged, null));
            line(out, "  the provider's own records for A-1001: " + provider.status("A-1001") + "  <- MARKED PAID, NEVER PAID");
            line(out, "  the webhook address is public, so a message to it proves nothing about who sent it");
        }
    }

    private static void act2Signatures(StringBuilder out) throws IOException, InterruptedException {
        line(out, "TWO. A signature over the exact bytes.");
        SimClock clock = new SimClock(START);
        Provider provider = new Provider(clock, SECRET);
        Receiver receiver = new Receiver(Receiver.Policy.signedOnly(), clock, orders(), SECRET);
        Provider.Delivery real = provider.send(new Event("evt_1001paid", Event.PAID, "A-1001", 4200));
        line(out, "  the provider signs HMAC-SHA256 of the timestamp and the raw body with the secret it shares with the store");
        line(out, "  the real event, as signed: " + receiver.receive(real.body(), real.signature()));
        byte[] changed = new String(real.body(), StandardCharsets.UTF_8).replace("A-1001", "A-1002").getBytes(StandardCharsets.UTF_8);
        line(out, "  the same signature on a body changed to order A-1002: " + receiver.receive(changed, real.signature()));
        byte[] forged = new Event("evt_forged2", Event.PAID, "A-1002", 1999).toJson();
        line(out, "  a forged event with no signature: " + receiver.receive(forged, null));
        line(out, "  a forged event signed with a secret that is not the provider's: "
                + receiver.receive(forged, Signatures.header(OTHER_SECRET, clock.now(), forged)));
        line(out, "  the comparison takes the same time however many bytes match (MessageDigest.isEqual)");
        Receiver overHttp = new Receiver(Receiver.Policy.signedOnly(), clock, orders(), SECRET);
        try (WebhookServer server = new WebhookServer(overHttp)) {
            line(out, "  the same checks over the local HTTP server: real event " + post(server.endpoint(), real.body(), real.signature()).status()
                    + ", changed body " + post(server.endpoint(), changed, real.signature()).status()
                    + ", no signature " + post(server.endpoint(), forged, null).status());
        }
    }

    private static void act3Replays(StringBuilder out) {
        line(out, "THREE. A real message sent again.");
        SimClock clock = new SimClock(START);
        Provider provider = new Provider(clock, SECRET);
        Orders orders = orders();
        Receiver signedOnly = new Receiver(Receiver.Policy.signedOnly(), clock, orders, SECRET);
        Provider.Delivery paid = provider.send(new Event("evt_1001paid", Event.PAID, "A-1001", 4200));
        signedOnly.receive(paid.body(), paid.signature());
        Provider.Delivery refund = provider.send(new Event("evt_1001refund", Event.REFUNDED, "A-1001", 4200));
        line(out, "  with signatures only, a real refund: " + signedOnly.receive(refund.body(), refund.signature()));
        clock.advance(3600);
        line(out, "  someone who saw that message sends a copy an hour later: " + signedOnly.receive(refund.body(), refund.signature()));
        line(out, "  refunds paid for A-1001: " + orders.refunds("A-1001") + ", " + orders.refundedCents("A-1001")
                + " cents for a 4200-cent order  <- A REAL MESSAGE, REPLAYED");

        clock = new SimClock(START);
        provider = new Provider(clock, SECRET);
        orders = orders();
        Receiver complete = new Receiver(Receiver.Policy.complete(), clock, orders, SECRET);
        paid = provider.send(new Event("evt_1001paid", Event.PAID, "A-1001", 4200));
        complete.receive(paid.body(), paid.signature());
        refund = provider.send(new Event("evt_1001refund", Event.REFUNDED, "A-1001", 4200));
        line(out, "  with a 300-second window on the signed time, and a record of processed event ids:");
        line(out, "    the real refund: " + complete.receive(refund.body(), refund.signature()));
        clock.advance(30);
        line(out, "    the same message again after 30 seconds: " + complete.receive(refund.body(), refund.signature()));
        clock.advance(3600);
        line(out, "    the same message again an hour later: " + complete.receive(refund.body(), refund.signature()));
        String newTime = refund.signature().replaceFirst("t=[0-9]+", "t=" + clock.now());
        line(out, "    the same message with its time edited to now: " + complete.receive(refund.body(), newTime)
                + ", because the time is part of what is signed");
        line(out, "    refunds paid for A-1001: " + orders.refunds("A-1001") + ", " + orders.refundedCents("A-1001") + " cents");
    }

    private static void act4Idempotency(StringBuilder out) {
        line(out, "FOUR. The same event twice, events out of order, and a new secret.");
        SimClock clock = new SimClock(START);
        Provider provider = new Provider(clock, SECRET);
        Orders orders = orders();
        Receiver receiver = new Receiver(Receiver.Policy.complete(), clock, orders, SECRET);
        Provider.Delivery paid = provider.send(new Event("evt_1001paid", Event.PAID, "A-1001", 4200));
        receiver.receive(paid.body(), paid.signature());
        receiver.receive(paid.body(), paid.signature());
        line(out, "  the provider retries the payment event, so it arrives twice: order changes " + orders.changes() + " time, A-1001 is "
                + orders.state("A-1001").name().toLowerCase(Locale.ROOT));
        Provider.Delivery refundFirst = provider.send(new Event("evt_1002refund", Event.REFUNDED, "A-1002", 1999));
        line(out, "  a refund for A-1002 arrives before its payment: " + receiver.receive(refundFirst.body(), refundFirst.signature()));
        Provider.Delivery payment = provider.send(new Event("evt_1002paid", Event.PAID, "A-1002", 1999));
        line(out, "  then the payment: " + receiver.receive(payment.body(), payment.signature()));
        line(out, "  then the provider's retry of the refund: " + receiver.receive(refundFirst.body(), refundFirst.signature()));

        SimClock rotation = new SimClock(START);
        Provider shop = new Provider(rotation, SECRET);
        Receiver store = new Receiver(Receiver.Policy.complete(), rotation, orders(), SECRET);
        Provider.Delivery signedBefore = shop.send(new Event("evt_old", Event.PAID, "A-1001", 4200));
        store.addSecret(NEW_SECRET);
        shop.rotateTo(NEW_SECRET);
        Provider.Delivery signedAfter = shop.send(new Event("evt_new", Event.PAID, "A-1002", 1999));
        line(out, "  changing the secret: the store accepts both for a while; a message signed with the old secret: "
                + store.receive(signedBefore.body(), signedBefore.signature()).status()
                + ", with the new one: " + store.receive(signedAfter.body(), signedAfter.signature()).status());
        store.removeSecret(SECRET);
        Provider.Delivery late = new Provider(rotation, SECRET).send(new Event("evt_late", Event.REFUNDED, "A-1001", 4200));
        line(out, "  after the old secret is removed, a message signed with it: " + store.receive(late.body(), late.signature()));
    }

    private static void act5TheBill(StringBuilder out) {
        line(out, "FIVE. The bill.");
        SimClock clock = new SimClock(START);
        Provider provider = new Provider(clock, SECRET);
        Receiver receiver = new Receiver(Receiver.Policy.complete(), clock, orders(), SECRET);
        Event event = new Event("evt_1001paid", Event.PAID, "A-1001", 4200);
        Provider.Delivery real = provider.send(event);
        line(out, "  a framework that parses the body and writes it again with other spacing, then checks the signature: "
                + receiver.receive(event.toPrettyJson(), real.signature()));
        line(out, "  so the raw bytes must be kept, and checked, before anything parses them");
        line(out, "  the real event: " + receiver.receive(real.body(), real.signature()));
        line(out, "  and before shipping, the store asks the provider's API about A-1001: " + provider.status("A-1001")
                + ", which matches the event");
        line(out, "  the shared secret lives outside the code, is changed on a schedule with a changeover, and leaks are a forged-payment risk");
        line(out, "  events can arrive late, twice or out of order, so every handler must be idempotent and check the order's state");
    }
}
