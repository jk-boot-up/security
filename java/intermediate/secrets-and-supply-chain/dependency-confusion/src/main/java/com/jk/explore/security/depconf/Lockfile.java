package com.jk.explore.security.depconf;

import java.util.LinkedHashMap;
import java.util.Map;

/** The exact version and digest of every dependency, recorded from a build someone trusted. A later build must get the same bytes. */
public final class Lockfile {

    public record Entry(Version version, String digest) {
    }

    private final Map<String, Entry> entries = new LinkedHashMap<>();

    public Lockfile record(Resolved r) {
        entries.put(r.artifact().name(), new Entry(r.artifact().version(), r.artifact().digest()));
        return this;
    }

    public Entry get(String name) {
        return entries.get(name);
    }

    public int size() {
        return entries.size();
    }
}
