package com.jk.explore.security.webhooks;

import java.util.HashMap;
import java.util.Map;

/** The payment provider, simulated: it records what really happened, signs the events it sends, and answers a status query from the store. */
final class Provider {

    /** A webhook as sent: the raw body and its signature header. */
    record Delivery(byte[] body, String signature) {
    }

    private final SimClock clock;
    private byte[] secret;
    private final Map<String, String> paymentStatus = new HashMap<>();

    Provider(SimClock clock, byte[] secret) {
        this.clock = clock;
        this.secret = secret;
    }

    void rotateTo(byte[] newSecret) {
        this.secret = newSecret;
    }

    /** Records an event in the provider's own books and returns it signed, ready to send. */
    Delivery send(Event event) {
        paymentStatus.put(event.order(), event.type() + " " + event.amount());
        byte[] body = event.toJson();
        return new Delivery(body, Signatures.header(secret, clock.now(), body));
    }

    /** The provider's API: what it says about an order, which is the final word on whether money moved. */
    String status(String order) {
        return paymentStatus.getOrDefault(order, "no payment");
    }
}
