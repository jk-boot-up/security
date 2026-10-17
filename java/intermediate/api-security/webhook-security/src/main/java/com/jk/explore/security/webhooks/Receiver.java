package com.jk.explore.security.webhooks;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The store's webhook endpoint logic, with each check switched on by its {@link Policy}. The order of the checks matters: the signature is
 * checked on the raw bytes before anything is parsed, the time before the event id, and an event is remembered only once it has been applied.
 */
final class Receiver {

    /** Which checks are on. A tolerance of zero or less means timestamps are not checked. */
    record Policy(boolean verifySignature, long toleranceSeconds, boolean rememberEvents, boolean checkState) {

        static Policy trusting() {
            return new Policy(false, 0, false, false);
        }

        static Policy signedOnly() {
            return new Policy(true, 0, false, false);
        }

        static Policy complete() {
            return new Policy(true, 300, true, true);
        }
    }

    private final Policy policy;
    private final SimClock clock;
    private final Orders orders;
    private final List<byte[]> secrets = new ArrayList<>();
    private final Set<String> processed = new HashSet<>();

    Receiver(Policy policy, SimClock clock, Orders orders, byte[]... secrets) {
        this.policy = policy;
        this.clock = clock;
        this.orders = orders;
        this.secrets.addAll(List.of(secrets));
    }

    /** Starts accepting a second secret, for a changeover to a new one. */
    void addSecret(byte[] secret) {
        secrets.add(secret);
    }

    /** Stops accepting a secret, once the provider signs only with the new one. */
    void removeSecret(byte[] secret) {
        secrets.removeIf(s -> MessageDigest.isEqual(s, secret));
    }

    Reply receive(byte[] rawBody, String signatureHeader) {
        if (policy.verifySignature()) {
            Optional<Signatures.Parsed> parsed = Signatures.parse(signatureHeader);
            if (parsed.isEmpty()) {
                return new Reply(400, "refused: no signature");
            }
            if (policy.toleranceSeconds() > 0 && Math.abs(clock.now() - parsed.get().timestamp()) > policy.toleranceSeconds()) {
                return new Reply(400, "refused: signed too long ago");
            }
            if (!Signatures.matchesAny(secrets, parsed.get(), rawBody)) {
                return new Reply(401, "refused: the signature does not match");
            }
        }
        Optional<Event> event = Event.parse(rawBody);
        if (event.isEmpty()) {
            return new Reply(400, "refused: not an event");
        }
        if (policy.rememberEvents() && processed.contains(event.get().id())) {
            return new Reply(200, "already processed " + event.get().id() + ", nothing changed");
        }
        String outcome = orders.apply(event.get(), policy.checkState());
        if (outcome.startsWith("not applied")) {
            // answered with an error so that the provider sends it again later; not remembered, so the retry can still apply
            return new Reply(409, outcome + ", try again later");
        }
        if (outcome.startsWith("refused")) {
            return new Reply(422, outcome);
        }
        processed.add(event.get().id());
        return new Reply(200, outcome);
    }
}
