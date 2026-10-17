package com.jk.explore.security.webhooks;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A payment event: its unique id, its type, the order and the amount in cents. The wire form is a small JSON object. */
record Event(String id, String type, String order, long amount) {

    static final String PAID = "payment.succeeded";
    static final String REFUNDED = "payment.refunded";

    private static final Pattern WIRE = Pattern.compile(
            "\\{\\s*\"id\"\\s*:\\s*\"(evt_[A-Za-z0-9]{1,32})\"\\s*,\\s*\"type\"\\s*:\\s*\"(payment\\.(?:succeeded|refunded))\"\\s*,"
                    + "\\s*\"order\"\\s*:\\s*\"(A-[0-9]{1,9})\"\\s*,\\s*\"amount\"\\s*:\\s*([0-9]{1,12})\\s*}");

    byte[] toJson() {
        return ("{\"id\":\"" + id + "\",\"type\":\"" + type + "\",\"order\":\"" + order + "\",\"amount\":" + amount + "}")
                .getBytes(StandardCharsets.UTF_8);
    }

    /** The same event written again with different spacing, as a parser and re-serializer would. Same meaning, different bytes. */
    byte[] toPrettyJson() {
        return ("{ \"id\": \"" + id + "\", \"type\": \"" + type + "\", \"order\": \"" + order + "\", \"amount\": " + amount + " }")
                .getBytes(StandardCharsets.UTF_8);
    }

    /** Strict parsing of the one shape the store accepts; anything else is not an event. */
    static Optional<Event> parse(byte[] raw) {
        Matcher m = WIRE.matcher(new String(raw, StandardCharsets.UTF_8));
        if (!m.matches()) {
            return Optional.empty();
        }
        return Optional.of(new Event(m.group(1), m.group(2), m.group(3), Long.parseLong(m.group(4))));
    }
}
