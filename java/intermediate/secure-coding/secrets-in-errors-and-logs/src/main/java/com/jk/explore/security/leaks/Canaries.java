package com.jk.explore.security.leaks;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Known fake secrets sent through the flow on purpose. Finding one in a response or a log line is a leak, which a test can fail on. */
public final class Canaries {

    private final Map<String, String> secrets;

    /** {@code secrets} maps a name ("card number") to the fake value; the order is kept. */
    public Canaries(Map<String, String> secrets) {
        this.secrets = new LinkedHashMap<>(secrets);
    }

    public List<String> names() {
        return List.copyOf(secrets.keySet());
    }

    public boolean foundIn(String name, String text) {
        return text.contains(secrets.get(name));
    }

    public boolean anyFoundIn(String text) {
        return secrets.values().stream().anyMatch(text::contains);
    }
}
