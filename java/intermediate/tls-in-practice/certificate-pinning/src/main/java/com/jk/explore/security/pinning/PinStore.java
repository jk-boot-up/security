package com.jk.explore.security.pinning;

import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dynamic pinning as browsers once did it (HPKP): the <em>server</em> tells the browser which keys to pin and for how long, in a response header, and the
 * browser remembers it. Whoever controls one response, on the real site or in a meddler's hands on a first visit, sets a pin that lasts up to a year.
 */
public final class PinStore {

    private record Entry(Set<String> pins, Instant expires) {
    }

    private final Map<String, Entry> entries = new HashMap<>();

    /** Reads {@code Public-Key-Pins: pin-sha256="..."; pin-sha256="..."; max-age=N}. */
    public void onResponse(String host, String headerValue, Instant now) {
        long maxAge = 0;
        java.util.HashSet<String> pins = new java.util.HashSet<>();
        for (String part : headerValue.split(";")) {
            String p = part.trim();
            if (p.startsWith("pin-sha256=")) {
                pins.add(p.substring("pin-sha256=".length()).replace("\"", ""));
            } else if (p.startsWith("max-age=")) {
                maxAge = Long.parseLong(p.substring("max-age=".length()));
            }
        }
        if (pins.size() >= 2 && maxAge > 0) { // at least two pins were required, one of them meant to be a backup
            entries.put(host, new Entry(pins, now.plus(Duration.ofSeconds(maxAge))));
        } else if (maxAge == 0) {
            entries.remove(host);
        }
    }

    /** True if the chain may be used for this host now. */
    public boolean allows(String host, List<X509Certificate> chain, Instant now) {
        Entry e = entries.get(host);
        if (e == null || now.isAfter(e.expires())) {
            return true;
        }
        return chain.stream().anyMatch(c -> e.pins().contains(Spki.pin(c)));
    }
}
