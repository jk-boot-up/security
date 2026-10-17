package com.jk.explore.security.webhooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ReceiverTest {

    private static final byte[] SECRET = "s1".getBytes(StandardCharsets.UTF_8);
    private static final byte[] NEXT = "s2".getBytes(StandardCharsets.UTF_8);

    private final SimClock clock = new SimClock(1000);
    private final Provider provider = new Provider(clock, SECRET);
    private final Orders orders = new Orders().add("A-1", 100).add("A-2", 50);

    private Receiver complete() {
        return new Receiver(Receiver.Policy.complete(), clock, orders, SECRET);
    }

    private static Event paid(String id, String order, long amount) {
        return new Event(id, Event.PAID, order, amount);
    }

    @Test
    void aTrustingEndpointActsOnAnUnsignedMessage() {
        Receiver r = new Receiver(Receiver.Policy.trusting(), clock, orders);
        assertEquals(200, r.receive(paid("evt_x", "A-1", 100).toJson(), null).status());
        assertEquals(Orders.State.PAID, orders.state("A-1"));
    }

    @Test
    void missingWrongAndChangedSignaturesAreRefusedBeforeAnythingChanges() {
        Receiver r = complete();
        byte[] body = paid("evt_1", "A-1", 100).toJson();
        assertEquals(400, r.receive(body, null).status());
        assertEquals(401, r.receive(body, Signatures.header("nope".getBytes(StandardCharsets.UTF_8), clock.now(), body)).status());
        Provider.Delivery real = provider.send(paid("evt_1", "A-1", 100));
        byte[] changed = new String(real.body(), StandardCharsets.UTF_8).replace("A-1", "A-2").getBytes(StandardCharsets.UTF_8);
        assertEquals(401, r.receive(changed, real.signature()).status());
        assertEquals(0, orders.changes());
        assertEquals(200, r.receive(real.body(), real.signature()).status());
        assertEquals(1, orders.changes());
    }

    @Test
    void theWindowIsMeasuredBothWays() {
        Receiver r = complete();
        Provider.Delivery d = provider.send(paid("evt_1", "A-1", 100));
        clock.advance(301);
        assertEquals(400, r.receive(d.body(), d.signature()).status());
        SimClock behind = new SimClock(1000);
        Receiver early = new Receiver(Receiver.Policy.complete(), behind, orders, SECRET);
        Provider.Delivery future = new Provider(new SimClock(1301), SECRET).send(paid("evt_2", "A-1", 100));
        assertEquals(400, early.receive(future.body(), future.signature()).status());
        Provider.Delivery edge = new Provider(new SimClock(1300), SECRET).send(paid("evt_3", "A-1", 100));
        assertEquals(200, early.receive(edge.body(), edge.signature()).status());
    }

    @Test
    void aProcessedEventIsNotAppliedAgain() {
        Receiver r = complete();
        Provider.Delivery d = provider.send(paid("evt_1", "A-1", 100));
        assertEquals(200, r.receive(d.body(), d.signature()).status());
        Reply again = r.receive(d.body(), d.signature());
        assertEquals(200, again.status());
        assertTrue(again.message().startsWith("already processed"));
        assertEquals(1, orders.changes());
    }

    @Test
    void withoutReplayChecksARefundIsPaidTwice() {
        Receiver r = new Receiver(Receiver.Policy.signedOnly(), clock, orders, SECRET);
        Provider.Delivery pay = provider.send(paid("evt_1", "A-1", 100));
        r.receive(pay.body(), pay.signature());
        Provider.Delivery refund = provider.send(new Event("evt_2", Event.REFUNDED, "A-1", 100));
        r.receive(refund.body(), refund.signature());
        clock.advance(86_400);
        r.receive(refund.body(), refund.signature());
        assertEquals(2, orders.refunds("A-1"));
        assertEquals(200, orders.refundedCents("A-1"));
    }

    @Test
    void anEventOutOfOrderIsRetriedLaterAndThenApplied() {
        Receiver r = complete();
        Provider.Delivery refund = provider.send(new Event("evt_r", Event.REFUNDED, "A-2", 50));
        assertEquals(409, r.receive(refund.body(), refund.signature()).status());
        Provider.Delivery pay = provider.send(paid("evt_p", "A-2", 50));
        assertEquals(200, r.receive(pay.body(), pay.signature()).status());
        assertEquals(200, r.receive(refund.body(), refund.signature()).status());
        assertEquals(Orders.State.REFUNDED, orders.state("A-2"));
        assertEquals(1, orders.refunds("A-2"));
    }

    @Test
    void wrongAmountsUnknownOrdersAndOddBodiesAreRefused() {
        Receiver r = complete();
        Provider.Delivery wrongAmount = provider.send(paid("evt_1", "A-1", 99));
        assertEquals(422, r.receive(wrongAmount.body(), wrongAmount.signature()).status());
        Provider.Delivery unknown = provider.send(paid("evt_2", "A-9", 1));
        assertEquals(422, r.receive(unknown.body(), unknown.signature()).status());
        byte[] odd = "{\"id\":\"evt_3\",\"type\":\"payment.succeeded\",\"order\":\"A-1\",\"amount\":100,\"extra\":1}".getBytes(StandardCharsets.UTF_8);
        assertEquals(400, r.receive(odd, Signatures.header(SECRET, clock.now(), odd)).status());
        assertEquals(0, orders.changes());
    }

    @Test
    void bothSecretsWorkDuringAChangeoverAndOnlyTheNewOneAfter() {
        Receiver r = complete();
        Provider.Delivery before = provider.send(paid("evt_1", "A-1", 100));
        r.addSecret(NEXT);
        provider.rotateTo(NEXT);
        Provider.Delivery after = provider.send(paid("evt_2", "A-2", 50));
        assertEquals(200, r.receive(before.body(), before.signature()).status());
        assertEquals(200, r.receive(after.body(), after.signature()).status());
        r.removeSecret(SECRET);
        Provider.Delivery old = new Provider(clock, SECRET).send(new Event("evt_3", Event.REFUNDED, "A-1", 100));
        assertEquals(401, r.receive(old.body(), old.signature()).status());
    }

    @Test
    void reformattedBodiesFailTheSignature() {
        Receiver r = complete();
        Event e = paid("evt_1", "A-1", 100);
        Provider.Delivery d = provider.send(e);
        assertEquals(401, r.receive(e.toPrettyJson(), d.signature()).status());
        assertEquals(e, Event.parse(e.toPrettyJson()).orElseThrow());
    }
}
